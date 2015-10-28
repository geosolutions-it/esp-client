package org.esp.publisher.form;

import it.jrc.auth.RoleManager;
import it.jrc.form.FieldGroup;
import it.jrc.form.controller.EditorController;
import it.jrc.form.view.IEditorView;
import it.jrc.persist.Dao;
import it.jrc.ui.HtmlLabel;

import java.util.List;

import org.esp.domain.blueprint.Message;
import org.esp.domain.blueprint.Message_;
import org.esp.server.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class MessageEditorController extends EditorController<Message> {

    private static Logger logger = LoggerFactory.getLogger(MessageEditorController.class);

    private MailService mailService;

    private Window window;

    private Button send;

    private MessageEditorView messageEditorView;

    private enum WINDOW_TYPE {
        DIRECT, FEEDBACK, SHOW;
    };

    private WINDOW_TYPE windowType;

    private RoleManager roleManager;

    @Inject
    public MessageEditorController(Dao dao, RoleManager roleManager, MailService mailService) {
        super(Message.class, dao);
        this.roleManager = roleManager;
        this.mailService = mailService;
        TextArea textField = ff.addTextArea(Message_.text);
        textField.setCaption(null);
        textField.setMaxLength(500);
        textField.setSizeFull();
        textField.setInputPrompt("Write message to send");
        textField.setHeight("100%");
        textField.addTextChangeListener(new FieldEvents.TextChangeListener() {
            @Override
            public void textChange(TextChangeEvent event) {
                String value = event.getText();
                if (value == null || value.isEmpty()) {
                    send.setEnabled(false);
                } else {
                    send.setEnabled(true);
                }
            }
        });
        addFieldGroup("Message");
    }

    @Override
    public void init(IEditorView<Message> view) {
        setSizeFull();

        MessageEditorView messageEditorView = (MessageEditorView) view;
        this.messageEditorView = messageEditorView;
        setContent(messageEditorView);

        List<FieldGroup<Message>> fieldGroups = getFieldGroups();
        view.buildForm(fieldGroups);

        buildSendPanel(messageEditorView.getSendPanel());

    }

    protected void buildSendPanel(VerticalLayout panel) {
        send = new Button("Send");
        send.setEnabled(false);
        send.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                commitForm(false);
            }
        });
        panel.addComponent(send);
        panel.setComponentAlignment(send, Alignment.MIDDLE_CENTER);
    }

    @Override
    protected void doPostCommit(Message entity) {
        super.doPostCommit(entity);

        try {
            if (windowType == WINDOW_TYPE.DIRECT) {
                mailService.sendBackEmailMessage(entity, roleManager.getRole(), entity
                        .getEcosystemServiceIndicator().getRole());
            }
            if (windowType == WINDOW_TYPE.FEEDBACK) {
                mailService.sendBackEmailMessage(entity, roleManager.getRole(), entity.getParent()
                        .getAuthor());
            }
            Notification.show("Message sent", Notification.Type.WARNING_MESSAGE);
        } catch (Exception e) {
            Notification.show("Message added to board, but email fails",
                    Notification.Type.ERROR_MESSAGE);
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        } finally {
            if (window != null) {
                window.close();
            }
        }

    }

    public void setContainer(Window window) {
        this.window = window;
    }

    public void isDirect() {
        windowType = WINDOW_TYPE.DIRECT;
        messageEditorView.getSendPanel().setVisible(true);
        messageEditorView.getMainPanel().setVisible(true);
        messageEditorView.getInfoPanel().setVisible(false);
    }

    public void isFeedback() {
        windowType = WINDOW_TYPE.FEEDBACK;
        messageEditorView.getSendPanel().setVisible(true);
        messageEditorView.getMainPanel().setVisible(true);
        messageEditorView.getInfoPanel().setVisible(true);
        HtmlLabel receivedMessage = new HtmlLabel();
        StringBuilder sb = new StringBuilder();
        sb.append("<div><b>You have new message from ");
        sb.append(getEntity().getAuthor().getFirstName());
        sb.append(" ");
        sb.append(getEntity().getAuthor().getLastName());
        sb.append(":</b></div>");
        sb.append("<p align=\"justify\">");
        sb.append(getEntity().getParent().getText());
        sb.append("</p>");
        receivedMessage.setValue(sb.toString());
        messageEditorView.getInfoPanel().removeAllComponents();
        messageEditorView.getInfoPanel().addComponent(receivedMessage);
    }

    public void isMessageShow() {
        windowType = WINDOW_TYPE.SHOW;
        messageEditorView.getSendPanel().setVisible(false);
        messageEditorView.getMainPanel().setVisible(false);
        messageEditorView.getInfoPanel().setVisible(true);
        HtmlLabel receivedMessage = new HtmlLabel();
        StringBuilder sb = new StringBuilder();
        sb.append("<div><b>You have new message from ");
        sb.append(getEntity().getAuthor().getFirstName());
        sb.append(" ");
        sb.append(getEntity().getAuthor().getLastName());
        sb.append(":</b></div>");
        sb.append("<p align=\"justify\">");
        sb.append(getEntity().getText());
        sb.append("</p>");
        sb.append("<div><b>In response to your message: :</b></div>");
        sb.append("<p align=\"justify\">");
        sb.append(getEntity().getParent().getText());
        sb.append("</p>");
        receivedMessage.setValue(sb.toString());
        messageEditorView.getInfoPanel().removeAllComponents();
        messageEditorView.getInfoPanel().addComponent(receivedMessage);
    }

}
