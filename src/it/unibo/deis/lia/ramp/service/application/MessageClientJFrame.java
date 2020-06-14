
package it.unibo.deis.lia.ramp.service.application;


/**
 *
 * @author Carlo Giannelli
 */
public class MessageClientJFrame extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;
	
    private MessageClient mc;
    public MessageClientJFrame(MessageClient mc) {
        initComponents();

        this.mc=mc;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonSendMessage = new javax.swing.JButton();
        jScrollPaneRemoteServices = new javax.swing.JScrollPane();
        jTextAreaMessage = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldDestNodeId = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldPacketDeliveryTimeout = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MessageClient");
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jButtonSendMessage.setText("send message");
        jButtonSendMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSendMessageActionPerformed(evt);
            }
        });

        jTextAreaMessage.setColumns(20);
        jTextAreaMessage.setRows(5);
        jTextAreaMessage.setText("Lorem ipsum dolor sit amet...");
        jScrollPaneRemoteServices.setViewportView(jTextAreaMessage);

        jLabel1.setText("destNodeId");

        jTextFieldDestNodeId.setText("44-45-53-54-42-00");

        jLabel2.setText("packetDeliveryTimeout (seconds)");

        jTextFieldPacketDeliveryTimeout.setText("60");
        jTextFieldPacketDeliveryTimeout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPacketDeliveryTimeoutActionPerformed(evt);
            }
        });

        jLabel3.setText("(-1 to disable packet timeout)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneRemoteServices, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
                    .addComponent(jButtonSendMessage)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldDestNodeId, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPacketDeliveryTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonSendMessage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldDestNodeId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldPacketDeliveryTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneRemoteServices, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSendMessageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSendMessageActionPerformed
        String mess = this.jTextAreaMessage.getText();
        //int destNodeId = this.jTextFieldDestNodeId.getText().hashCode();
        int destNodeId = Integer.parseInt(this.jTextFieldDestNodeId.getText());
        int timeout = Integer.parseInt(this.jTextFieldPacketDeliveryTimeout.getText());

        mc.sendMessage(destNodeId, mess, timeout);
    }//GEN-LAST:event_jButtonSendMessageActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        mc.stopClient();
    }//GEN-LAST:event_formWindowClosing

    private void jTextFieldPacketDeliveryTimeoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPacketDeliveryTimeoutActionPerformed
        // do nothing...
    }//GEN-LAST:event_jTextFieldPacketDeliveryTimeoutActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonSendMessage;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPaneRemoteServices;
    private javax.swing.JTextArea jTextAreaMessage;
    private javax.swing.JTextField jTextFieldDestNodeId;
    private javax.swing.JTextField jTextFieldPacketDeliveryTimeout;
    // End of variables declaration//GEN-END:variables

}
