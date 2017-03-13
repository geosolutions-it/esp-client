package org.esp.publisher.colours;

import it.jrc.form.FieldGroup;
import it.jrc.form.editor.SubmitPanel;
import it.jrc.form.view.IEditorView;

import java.util.List;

import org.esp.domain.publisher.ColourMap;

import com.vaadin.ui.Field;
import com.vaadin.ui.VerticalLayout;

/**
 * The view.
 * 
 * @author Will Temperley
 * 
 */
public class ColourMapView extends VerticalLayout implements
        IEditorView<ColourMap> {

    private SubmitPanel sb;

    public ColourMapView() {
        sb = new SubmitPanel();
    }


    @Override
    public SubmitPanel getTopSubmitPanel() {
        return sb;

    }

    @Override
    public void buildForm(List<FieldGroup<ColourMap>> fields) {

        for (FieldGroup<ColourMap> fieldGroup : fields) {
            for (Field<?> f : fieldGroup.getFieldGroup().getFields()) {
                addComponent(f);
            }
        }

        addComponent(sb);
        
    }


    @Override
    public SubmitPanel getBottomSubmitPanel() {
        return sb;
    }


	@Override
	public void buildForm(List<FieldGroup<ColourMap>> fields, boolean isInspire) {
		// TODO Auto-generated method stub
		
	}
}