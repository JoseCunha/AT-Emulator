package com.celer.emul;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Simple Swing front-end for the AT emulator that abstracts the AT commands required to send
 * an SMS message. The UI keeps the classic console available so users can still see the commands
 * and the responses produced by the emulator.
 */
public final class ATGui extends JFrame {

   private static final long serialVersionUID = 1L;

   private final JTextField numberField = new JTextField();
   private final JTextArea messageArea = new JTextArea(5, 30);
   private final JTextArea consoleArea = new JTextArea();
   private final JButton sendButton = new JButton("Enviar SMS");

   private final PipedOutputStream commandPipe = new PipedOutputStream();

   private ATGui() throws IOException {
      super("AT Emulator - Interface gráfica");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      messageArea.setLineWrap(true);
      messageArea.setWrapStyleWord(true);

      consoleArea.setEditable(false);
      consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
      consoleArea.setLineWrap(true);
      consoleArea.setWrapStyleWord(true);

      JPanel form = buildFormPanel();
      JScrollPane messageScroll = new JScrollPane(messageArea);
      messageScroll.setBorder(BorderFactory.createTitledBorder("Mensagem"));
      JScrollPane consoleScroll = new JScrollPane(consoleArea);
      consoleScroll.setPreferredSize(new Dimension(480, 260));
      consoleScroll.setBorder(BorderFactory.createTitledBorder("Console do emulador"));

      add(form, BorderLayout.NORTH);
      add(messageScroll, BorderLayout.CENTER);
      add(consoleScroll, BorderLayout.SOUTH);

      pack();
      setLocationRelativeTo(null);

      initialiseEmulator();
   }

   private JPanel buildFormPanel() {
      JPanel panel = new JPanel(new GridBagLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(5, 5, 5, 5);
      gbc.fill = GridBagConstraints.HORIZONTAL;

      gbc.gridx = 0;
      gbc.gridy = 0;
      panel.add(new JLabel("Número"), gbc);

      gbc.gridx = 1;
      gbc.weightx = 1.0;
      numberField.setColumns(15);
      panel.add(numberField, gbc);

      gbc.gridx = 2;
      gbc.weightx = 0;
      JButton clearButton = new JButton("Limpar");
      clearButton.addActionListener(e -> messageArea.setText(""));
      panel.add(clearButton, gbc);

      gbc.gridx = 0;
      gbc.gridy = 1;
      panel.add(new JLabel("Comandos"), gbc);

      gbc.gridx = 1;
      gbc.gridwidth = 2;
      sendButton.addActionListener(e -> sendSms());
      panel.add(sendButton, gbc);

      return panel;
   }

   private void initialiseEmulator() throws IOException {
      PipedInputStream emulatorInput = new PipedInputStream(commandPipe, 8192);
      TextAreaOutputStream consoleStream = new TextAreaOutputStream();
      PrintStream emulatorOut = new PrintStream(consoleStream, true, StandardCharsets.UTF_8.name());
      Thread emulatorThread = new Thread(() -> AT.doLoop(new InputStreamReader(emulatorInput, StandardCharsets.UTF_8), emulatorOut, true),
         "AT-Emulator-Thread");
      emulatorThread.setDaemon(true);
      emulatorThread.start();

      sendRawCommand("AT");
      sendRawCommand("AT+CMGF=1");
   }

   private void sendSms() {
      String number = numberField.getText().trim();
      String text = messageArea.getText();

      if(number.isEmpty()) {
         JOptionPane.showMessageDialog(this, "Informe o número do destinatário.", "Número obrigatório", JOptionPane.WARNING_MESSAGE);
         numberField.requestFocusInWindow();
         return;
      }
      if(text == null || text.trim().isEmpty()) {
         JOptionPane.showMessageDialog(this, "Escreva uma mensagem para enviar.", "Mensagem obrigatória", JOptionPane.WARNING_MESSAGE);
         messageArea.requestFocusInWindow();
         return;
      }

      sendButton.setEnabled(false);
      appendToConsole(String.format(">> Enviando SMS para %s (%d caracteres)\n", number, text.length()));

      try {
         sendRawCommand("AT+CMGS=\"" + number + "\"");
         sendMessageBody(text);
      } catch(IOException ex) {
         JOptionPane.showMessageDialog(this, "Falha ao comunicar com o emulador: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
      } finally {
         sendButton.setEnabled(true);
      }
   }

   private void sendRawCommand(String command) throws IOException {
      appendToConsole(">> " + command + "\n");
      commandPipe.write(command.getBytes(StandardCharsets.UTF_8));
      commandPipe.write('\r');
      commandPipe.write('\n');
      commandPipe.flush();
   }

   private void sendMessageBody(String message) throws IOException {
      String sanitized = message.replace('\u001A', ' ');
      appendToConsole(">> " + sanitized + " ^Z\n");
      commandPipe.write(sanitized.getBytes(StandardCharsets.UTF_8));
      commandPipe.write(26);
      commandPipe.write('\r');
      commandPipe.write('\n');
      commandPipe.flush();
   }

   private void appendToConsole(String text) {
      SwingUtilities.invokeLater(() -> {
         consoleArea.append(text.replace("\r", ""));
         consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
      });
   }

   private final class TextAreaOutputStream extends OutputStream {
      @Override
      public void write(int b) throws IOException {
         write(new byte[] {(byte)b}, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
         if(len <= 0)
            return;
         String text = new String(b, off, len, StandardCharsets.UTF_8);
         appendToConsole(text);
      }
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         try {
            new ATGui().setVisible(true);
         } catch(IOException ex) {
            JOptionPane.showMessageDialog(null, "Não foi possível iniciar o emulador: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
         }
      });
   }
}
