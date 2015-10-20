package org.esp.publisher.form;

import it.jrc.form.FieldGroup;
import it.jrc.form.editor.SubmitPanel;
import it.jrc.form.view.IEditorView;

import java.util.Collection;
import java.util.List;

import org.esp.domain.blueprint.Message;

import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.ui.Field;
import com.vaadin.ui.VerticalLayout;

public class MessageEditorView extends VerticalLayout implements IEditorView<Message> {

    private VerticalLayout mainPanel;

    private VerticalLayout sendPanel;

    private VerticalLayout infoPanel;

    public MessageEditorView() {
        setSizeFull();
        setSpacing(true);

        infoPanel = new VerticalLayout();
        infoPanel.setHeight("100px");
        infoPanel.setVisible(false);
        addComponent(infoPanel);
        
        this.mainPanel = new VerticalLayout();
        mainPanel.setSizeFull();
        addComponent(mainPanel);
        
        sendPanel = new VerticalLayout();
        sendPanel.setHeight("30px");
        addComponent(sendPanel);

        setExpandRatio(mainPanel, 1);

    }

    public VerticalLayout getSendPanel() {
        return sendPanel;
    }
    
    public VerticalLayout getInfoPanel() {
        return infoPanel;
    }
    
    public VerticalLayout getMainPanel() {
        return mainPanel;
    }

    @Override
    public SubmitPanel getTopSubmitPanel() {
        return null;
    }

    @Override
    public SubmitPanel getBottomSubmitPanel() {
        return null;
    }

    @Override
    public void buildForm(List<FieldGroup<Message>> fields) {
        for (FieldGroup<Message> fieldGroupMeta : fields) {
            BeanFieldGroup<Message> bfg = fieldGroupMeta.getFieldGroup();
            Collection<Field<?>> fieldGroupFields = bfg.getFields();
            for (Field<?> field : fieldGroupFields) {
                mainPanel.addComponent(field);
            }
        }

    }

}
