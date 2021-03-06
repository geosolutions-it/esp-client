package org.esp.publisher.form;

import it.jrc.form.controller.EditorController;
import it.jrc.persist.Dao;

import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

public abstract class EditableField<T> extends CustomField<T> {

    protected Window w;
    protected Class<T> clazz;
    protected EditorController<T> editor;
    protected Dao dao;
    
    protected IAbstractSelect<Object> encapsulatedField;
    

    public EditableField(ComboBox2<?> f) {
        this.encapsulatedField = f;
    }

    private MenuBar getEditMenu() {
    
        MenuBar settings = new MenuBar();
        settings.setStyleName("field-menu");
        MenuItem settingsMenu = settings.addItem("", null);
        settingsMenu.setStyleName("icon-cog");
    
        settingsMenu.addItem("New", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                doCreate();
            }
        });
    
        settingsMenu.addItem("Edit", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                doUpdate();
            }
        });
        return settings;
    }



    @Override
    public void setInternalValue(T newValue) {
        encapsulatedField.setInternalValue(newValue);
        super.setInternalValue(newValue);
    }

    protected void populateCombo() {
        List<T> items = dao.all(clazz);
        for (T t : items) {
            encapsulatedField.addItem(t);
        }
    }

    @Override
    public void setPropertyDataSource(@SuppressWarnings("rawtypes") Property newDataSource) {
    
        encapsulatedField.setPropertyDataSource(newDataSource);
        super.setPropertyDataSource(newDataSource);
    }

    @Override
    public T getValue() {
        return (T) encapsulatedField.getValue();
    }

    protected Component initContent() {
        CssLayout l = new CssLayout();
        l.addStyleName("select-create-field");
        l.addComponent(encapsulatedField);
        l.addComponent(getEditMenu());
        return l;
    }

    protected void doCreate() {
        UI.getCurrent().addWindow(w);
        w.center();
        editor.doCreate();
    }

    protected void doUpdate() {
        Object value = encapsulatedField.getValue();
        if (value == null) {
            return;
        }
        UI.getCurrent().addWindow(w);
        w.center();
        editor.doUpdate((T) value);
    }

    @Override
    public Class<? extends T> getType() {
        return clazz;
    }

    public abstract void setEditor(EditorController<T> editor);

}