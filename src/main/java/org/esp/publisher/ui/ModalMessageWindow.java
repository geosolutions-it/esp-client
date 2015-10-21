package org.esp.publisher.ui;

import it.jrc.auth.RoleManager;

import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.Message;
import org.esp.publisher.form.MessageEditorController;
import org.esp.publisher.form.MessageEditorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.vaadin.ui.Window;

public class ModalMessageWindow extends Window {

    private static Logger logger = LoggerFactory.getLogger(ModalMessageWindow.class);

    private MessageEditorController messageEditorController;

    private RoleManager roleManager;

    @Inject
    public ModalMessageWindow(RoleManager roleManager,
            MessageEditorController messageEditorController, MessageEditorView messageEditorView) {
        super();
        this.roleManager = roleManager;
        this.messageEditorController = messageEditorController;
        this.messageEditorController.init(messageEditorView);
        this.messageEditorController.setContainer(this);
        this.setContent(this.messageEditorController);
        setStyleName("messagewindow");
        setModal(true);
        setDraggable(false);
        setResizable(false);
        center();
    }

    public void createMessage(EcosystemServiceIndicator selectedEntity) {
        setCaption("Send message to user");
        setHeight("300px");
        setWidth("400px");
        messageEditorController.doCreate();
        Message message = messageEditorController.getEntity();
        message.setAuthor(this.roleManager.getRole());
        message.setEcosystemServiceIndicator(selectedEntity);
        messageEditorController.isDirect();
    }
    
    public void feedbackMessage(Message message) {
        setCaption("Reply feedback to admin");
        setHeight("400px");
        setWidth("400px");
        messageEditorController.doCreate();
        Message feedback = messageEditorController.getEntity();
        feedback.setAuthor(this.roleManager.getRole());
        feedback.setEcosystemServiceIndicator(message.getEcosystemServiceIndicator());
        feedback.setParent(message);
        messageEditorController.isFeedback();
    }
    
    public void showMessage(Message message) {
        setCaption("Reply feedback from user");
        setHeight("300px");
        setWidth("400px");
        messageEditorController.doUpdate(message);
        messageEditorController.isMessageShow();
    }

    @Override
    public void addCloseListener(CloseListener listener) {
        super.addCloseListener(listener);
    }

}
