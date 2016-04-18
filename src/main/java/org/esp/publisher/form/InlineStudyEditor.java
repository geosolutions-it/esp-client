package org.esp.publisher.form;

import it.jrc.auth.RoleManager;
import it.jrc.form.component.YearField;
import it.jrc.form.controller.EditorController;
import it.jrc.persist.Dao;

import org.esp.domain.blueprint.Study;
import org.esp.domain.blueprint.Study_;

import com.google.inject.Inject;

public class InlineStudyEditor extends EditorController<Study> {


    private RoleManager roleManager;

    @Inject
    public InlineStudyEditor(final Dao dao, RoleManager roleManager) {

        super(Study.class, dao);

        this.roleManager = roleManager;

        ff.addField(Study_.studyName);
        ff.addField(Study_.url);
        ff.addField(Study_.studyPurpose);
        ff.addField(Study_.studyLocation);
        ff.addField(Study_.startYear, new YearField());
        ff.addField(Study_.endYear, new YearField());
        ff.addField(Study_.projectType);
        ff.addTextArea(Study_.projectReferences);
        ff.addTextArea(Study_.contactDetails);
        ff.addField(Study_.organizationName);
        ff.addField(Study_.email);
        ff.addField(Study_.phone);
        ff.addTextArea(Study_.fundingSource);
        ff.addTextArea(Study_.keywords);
        ff.addTextArea(Study_.mainInvestigators);

        addFieldGroup("Study");

    }
    
    @Override
    protected void doPreCommit(Study entity) {
        if (entity.getRole() == null) {
            entity.setRole(roleManager.getRole());
        }
    }

    @Override
    protected void doPostDelete(Study entity) {

//        Set<EcosystemServiceIndicator> esis = entity
//                .getEcosystemServiceIndicators();
//        if (esis != null) {
//            for (EcosystemServiceIndicator ecosystemServiceIndicator : esis) {
//
//                String layerName = ecosystemServiceIndicator.getLayerName();
//                if (layerName != null) {
//                    gsr.removeRasterStore(layerName);
//                }
//            }
//        }
//        getUI().getCurrent().getNavigator().navigateTo(ViewModule.HOME);
    }

}
