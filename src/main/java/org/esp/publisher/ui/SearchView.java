package org.esp.publisher.ui;

import it.jrc.auth.RoleManager;
import it.jrc.domain.auth.Role;
import it.jrc.form.editor.EntityTable;
import it.jrc.form.filter.FilterPanel;
import it.jrc.form.view.TwinPanelView;
import it.jrc.persist.ContainerManager;
import it.jrc.persist.Dao;
import it.jrc.ui.HtmlLabel;
import it.jrc.ui.SimplePanel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.EcosystemServiceIndicator_;
import org.esp.domain.blueprint.Message;
import org.esp.domain.blueprint.PublishStatus;
import org.esp.domain.blueprint.Status;
import org.esp.publisher.GeoserverRestApi;
import org.esp.publisher.LayerManager;
import org.vaadin.addon.leaflet.LMap;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.vaadin.addon.jpacontainer.EntityItem;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerItem;
import com.vaadin.data.Container.Filterable;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.filter.Compare.Equal;
import com.vaadin.data.util.filter.Or;
import com.vaadin.event.LayoutEvents;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.themes.BaseTheme;
import com.vividsolutions.jts.geom.Polygon;

public class SearchView extends TwinPanelView implements View {

    private JPAContainer<EcosystemServiceIndicator> esiContainer;

    private VerticalLayout content = new VerticalLayout();

    private Dao dao;

    private LayerManager layerManager;

    private MapLegend mapLegend;

    private EntityTable<EcosystemServiceIndicator> table;

    private EcosystemServiceIndicator selectedEntity;

    private RoleManager roleManager;

    private static int COL_WIDTH = 400;

    private static int CHECK_SIZE = 40;

    private Status validated;

    private Status notvalidated;

    private Button pb;

    private Button sb;

    private Set<JPAContainerItem<?>> checkedItems;

    private ModalMessageWindow modalMessageWindow;

    @Inject
    public SearchView(Dao dao, RoleManager roleManager, 
			final ModalMessageWindow modalMessageWindow,
            @Named("gs_wms_url") String defaultWms,
            GeoserverRestApi gsr) {

        ContainerManager<EcosystemServiceIndicator> containerManager = new ContainerManager<EcosystemServiceIndicator>(
                dao, EcosystemServiceIndicator.class);
        this.modalMessageWindow = modalMessageWindow;
        this.modalMessageWindow.addCloseListener(new Window.CloseListener() {
            @Override
            public void windowClose(CloseEvent e) {
                entitySelected(selectedEntity.getId());
            }
        });
        this.validated = dao.getEntityManager().find(Status.class,
                PublishStatus.VALIDATED.getValue());
        this.notvalidated = dao.getEntityManager().find(Status.class,
                PublishStatus.NOT_VALIDATED.getValue());
        this.checkedItems = new HashSet<JPAContainerItem<?>>();
        this.esiContainer = containerManager.getContainer();

        esiContainer.sort(new String[] { EcosystemServiceIndicator_.dateUpdated.getName() },
                new boolean[] { false });
        this.dao = dao;
        this.layerManager = new LayerManager(new LMap(), defaultWms);
        this.roleManager = roleManager;
        addPublishFilter(esiContainer);
        {
            SimplePanel leftPanel = getLeftPanel();
            leftPanel.setWidth((COL_WIDTH + 80) + "px");
            content.setSizeFull();
            leftPanel.addComponent(content);

            setExpandRatio(getRightPanel(), 1);

            FilterPanel<EcosystemServiceIndicator> filterPanel = getFilterPanel();

            HorizontalLayout hl = new HorizontalLayout();
            content.addComponent(hl);
            hl.setWidth("100%");
            HtmlLabel l = new HtmlLabel("Select a map to preview or edit.");

            hl.addComponent(l);
            l.setWidth("300px");
            hl.setExpandRatio(l, 1);

            content.addComponent(filterPanel);

            final EntityTable<EcosystemServiceIndicator> ecosystemServiceIndicatorTable = getEcosystemServiceIndicatorTable();

            /*
             * Top button bar
             */
            if (roleManager.getRole().getIsSuperUser()) {
                HorizontalLayout buttonBar = new HorizontalLayout();
                buttonBar.setSpacing(true);
                buttonBar.setHeight("50px");
                pb = new Button("Publish");
                pb.setEnabled(false);
                pb.addClickListener(new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        Iterator<JPAContainerItem<?>> i = checkedItems.iterator();
                        while (i.hasNext()) {
                            JPAContainerItem<?> item = i.next();
                            item.getItemProperty(EcosystemServiceIndicator_.status.getName())
                                    .setValue(validated);
                        }
                        esiContainer.refresh();
                        resetChecked();
                    }
                });
                buttonBar.addComponent(pb);
                buttonBar.setComponentAlignment(pb, Alignment.MIDDLE_CENTER);
                sb = new Button("Send back");
                sb.setEnabled(false);
                sb.addClickListener(new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        if (selectedEntity != null) {
                            modalMessageWindow.createMessage(selectedEntity);
                            UI.getCurrent().addWindow(modalMessageWindow);
                            modalMessageWindow.focus();
                        }
                    }
                });
                buttonBar.addComponent(sb);
                buttonBar.setComponentAlignment(sb, Alignment.MIDDLE_CENTER);
                content.addComponent(buttonBar);
            }

            /*
             * Table
             */

            content.addComponent(ecosystemServiceIndicatorTable);

            content.setExpandRatio(ecosystemServiceIndicatorTable, 1);

            /*
             * Bottom button bar
             */
            if (roleManager.getRole().getIsSuperUser()) {
                HorizontalLayout buttonBar = new HorizontalLayout();
                buttonBar.setSpacing(true);
                buttonBar.setHeight("50px");
                Button sa = new Button("Select all");
                sa.addClickListener(new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        Iterator<Component> i = ecosystemServiceIndicatorTable.iterator();
                        while (i.hasNext()) {
                            Component c = i.next();
                            if (c.getId() != null && c.getId().contains("published")
                                    && c instanceof CheckBox) {
                                CheckBox cb = (CheckBox) c;
                                cb.setValue(true);
                            }
                        }
                    }
                });
                buttonBar.addComponent(sa);
                buttonBar.setComponentAlignment(sa, Alignment.MIDDLE_CENTER);
                Button da = new Button("Deselect all");
                da.addClickListener(new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        Iterator<Component> i = ecosystemServiceIndicatorTable.iterator();
                        while (i.hasNext()) {
                            Component c = i.next();
                            if (c.getId() != null && c.getId().contains("published")
                                    && c instanceof CheckBox) {
                                CheckBox cb = (CheckBox) c;
                                cb.setValue(false);
                            }
                        }
                    }
                });
                buttonBar.addComponent(da);
                buttonBar.setComponentAlignment(da, Alignment.MIDDLE_CENTER);
                content.addComponent(buttonBar);
            }
        }

        /*
         * Right panel
         */
        {
            SimplePanel rightPanel = getRightPanel();

            HorizontalLayout vl = new HorizontalLayout();
            vl.setSizeFull();
            rightPanel.addComponent(vl);

            vl.addComponent(layerManager.getMap());
            layerManager.getMap().setSizeFull();

            HorizontalLayout hl = new HorizontalLayout();
            hl.setSizeFull();
            vl.addComponent(hl);

            mapLegend = new MapLegend(gsr);
            hl.addComponent(mapLegend);
            hl.setSpacing(true);
            
        }

    }

    private FilterPanel<EcosystemServiceIndicator> getFilterPanel() {

        FilterPanel<EcosystemServiceIndicator> fp = new FilterPanel<EcosystemServiceIndicator>(
                esiContainer, dao) {
            @Override
            public void doFiltering() {
                super.doFiltering();
                addPublishFilter(esiContainer);
            }
        };
        fp.addFilterField(EcosystemServiceIndicator_.ecosystemService);
        fp.addFilterField(EcosystemServiceIndicator_.study);
        return fp;

    }

    class ESPublishColumn implements Table.ColumnGenerator {

        public Component generateCell(Table source, Object itemId, Object columnId) {
            final JPAContainerItem<?> item = (JPAContainerItem<?>) source.getItem(itemId);
            final Object entity = item.getEntity();
            final EcosystemServiceIndicator esi = (EcosystemServiceIndicator) entity;
            /*
             * Check if map is not already published
             */
            boolean isPublished = (esi.getStatus() != null && esi.getStatus().getId() == PublishStatus.VALIDATED
                    .getValue());
            if (!isPublished) {
                final CheckBox checkBox = new CheckBox();
                checkBox.addValueChangeListener(new Property.ValueChangeListener() {
                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        if (checkBox.getValue()) {
                            checkedItems.add(item);
                        } else {
                            checkedItems.remove(item);
                        }
                        manageChecked();
                    }

                });
                checkBox.setId("published" + itemId.toString());
                return checkBox;
            } else {
                return new Label("");
            }
        }
    }

    class ESVisualizationColumn implements Table.ColumnGenerator {
        
        private Role role;

        public ESVisualizationColumn(Role role) {
            this.role = role;
        }

        public Component generateCell(Table source, final Object itemId,
                Object columnId) {
            JPAContainerItem<?> item = (JPAContainerItem<?>) source.getItem(itemId);

            HtmlLabel label = new HtmlLabel();

            if (item == null) {
                return label;
            }
            Object entity = item.getEntity();

            final EcosystemServiceIndicator esi = (EcosystemServiceIndicator) entity;
            
            /*
             * Check ownership
             */
            boolean isOwnerOrSupervisor = false;
            if (esi.getRole().equals(role) || role.getIsSuperUser()) {
                isOwnerOrSupervisor = true;
            }
            
            StringBuilder sb = new StringBuilder();

            if (isOwnerOrSupervisor) {
                sb.append("<a href='#!");
                sb.append(ViewModule.getESILink(esi));
                sb.append("'>");
                sb.append(esi.toString());
                sb.append("</a>");
            } else {
                sb.append("<span class='indicator-name'>");
                sb.append(esi.toString());
                sb.append("</span>");
            }
            
            sb.append("<br/>");
            sb.append(esi.getStudy().getStudyName());

            label.setValue(sb.toString());

            HorizontalLayout hl = new HorizontalLayout();
            hl.setSizeFull();
            hl.addComponent(label);
            hl.setExpandRatio(label, 1);
            hl.setComponentAlignment(label, Alignment.MIDDLE_LEFT);

            Component msgButton = getRecMessageBtn(esi);
            if (msgButton != null) {
                hl.addComponent(msgButton);
                hl.setComponentAlignment(msgButton, Alignment.MIDDLE_CENTER);
            }
            // Forward clicks on the layout as selection
            // in the table
            hl.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
                @Override
                public void layoutClick(LayoutClickEvent event) {
                    table.select(itemId);
                }
            });

            return hl;

        }

        public Component getRecMessageBtn(EcosystemServiceIndicator esi) {
            /*
             * Check if exists direct or feedback message
             */
            final Message fbMsg = getFeedbackMessage(esi);
            if (roleManager.getRole().getIsSuperUser() && fbMsg != null) {
                final Button readNewMessageBtn = new Button();
                readNewMessageBtn.setStyleName(BaseTheme.BUTTON_LINK);
                readNewMessageBtn.setIcon(new ThemeResource("../biopama/img/opened_email.png"));
                readNewMessageBtn.addClickListener(new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        modalMessageWindow.showMessage(fbMsg);
                        UI.getCurrent().addWindow(modalMessageWindow);
                        modalMessageWindow.focus();
                    }
                });
                return readNewMessageBtn;
            }
            final Message dirMsg = getDirectMessage(esi);
            if (!roleManager.getRole().getIsSuperUser() && dirMsg != null) {
                final Button readNewMessageBtn = new Button();
                readNewMessageBtn.setStyleName(BaseTheme.BUTTON_LINK);
                readNewMessageBtn.setIcon(new ThemeResource("../biopama/img/opened_email.png"));
                readNewMessageBtn.addClickListener(new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        modalMessageWindow.feedbackMessage(dirMsg);
                        UI.getCurrent().addWindow(modalMessageWindow);
                        modalMessageWindow.focus();
                    }
                });
                return readNewMessageBtn;
            }
            return null;
        }

    }

    @Override
    public void enter(ViewChangeEvent event) {
        if (selectedEntity == null) {

            Iterator<?> it = table.getItemIds().iterator();
            if (it.hasNext()) {
                Object next = it.next();
                EcosystemServiceIndicator obj = dao.find(EcosystemServiceIndicator.class, next);
                selectedEntity = obj;
                table.select(next);
            }

        }
    }

    private EntityTable<EcosystemServiceIndicator> getEcosystemServiceIndicatorTable() {

        table = new EntityTable<EcosystemServiceIndicator>(esiContainer);
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);

        table.setHeight("100%");
        table.setPageLength(20);

        table.addValueChangeListener(new Property.ValueChangeListener() {
            public void valueChange(Property.ValueChangeEvent event) {

                Long si = (Long) event.getProperty().getValue();
                entitySelected(si);

            }
        });

        /*
         * Check super user
         */
        boolean isSuperUser = roleManager.getRole().getIsSuperUser();

        if (isSuperUser) {
            ESPublishColumn publishGeneratedColumn = new ESPublishColumn();
            table.addGeneratedColumn("publish", publishGeneratedColumn);
            table.setColumnWidth("publish", CHECK_SIZE);
        }

        ESVisualizationColumn visualizationGeneratedColumn = new ESVisualizationColumn(
                roleManager.getRole());
        table.addGeneratedColumn("id", visualizationGeneratedColumn);
        table.setColumnWidth("id", COL_WIDTH - (isSuperUser ? CHECK_SIZE : 0));

        return table;

    }

    protected void addPublishFilter(Filterable toFilter) {
        if (!roleManager.getRole().getIsSuperUser()) {
            toFilter.addContainerFilter(new Or(new Equal(EcosystemServiceIndicator_.status
                    .getName(), PublishStatus.VALIDATED.getValue()), new Equal(
                    EcosystemServiceIndicator_.role.getName(), roleManager.getRole())));
        }
    }

    protected void entitySelected(Long id) {
        
        if (id == null) {
            // Todo - nothing is selected - clear the view, or not?
            resetSelected();
            return;
        }

        EntityItem<EcosystemServiceIndicator> x = esiContainer.getItem(id);
        EcosystemServiceIndicator entity = x.getEntity();
        
        this.selectedEntity = entity;

        if (entity == null) {
            return;
        }

        String layerName = entity.getLayerName();
        if (layerName != null) {
            layerManager.setSurfaceLayerName(layerName, entity.getTimestamp());
        } else {
            layerManager.setSurfaceLayerName("", 0);
        }

        Polygon env = entity.getEnvelope();
        if (env != null) {
            layerManager.zoomTo(env);
        }

        mapLegend.setValue(entity);

        /*
         * Disable send button if message is already sent by admin to this map or if the author of map is the admin itself
         */
        if (roleManager.getRole().getIsSuperUser()) {
            if (getSentMessage(entity) == null && !entity.getRole().equals(roleManager.getRole())) {
                sb.setEnabled(true);
            } else {
                sb.setEnabled(false);
            }
        }

    }

    private void resetSelected() {
        sb.setEnabled(false);
        selectedEntity = null;
    }

    private void resetChecked() {
        pb.setEnabled(false);
        checkedItems.clear();
    }

    private void manageChecked() {
        if (checkedItems.size() > 0) {
            pb.setEnabled(true);
        } else {
            pb.setEnabled(false);
        }
    }

    private Message getFeedbackMessage(EcosystemServiceIndicator esi) {
        Message fbkMsg = null;
        Set<Message> msgs = esi.getMessages();
        for (Message msg : msgs) {
            if (msg != null) {
                fbkMsg = msg.getFeedback();
                if (fbkMsg != null && msg.getAuthor().equals(this.roleManager.getRole())) {
                    break;
                }
            }
        }
        return fbkMsg;
    }

    /*
     * Hide message already managed by user (with feedback)
     */
    private Message getDirectMessage(EcosystemServiceIndicator esi) {
        Message dirMsg = null;
        Set<Message> msgs = esi.getMessages();
        for (Message msg : msgs) {
            if (msg != null && msg.getFeedback() == null && msg.getParent() == null) {
                dirMsg = msg;
                break;
            }
        }
        return dirMsg;
    }

    /*
     * Hide message already managed by user (with feedback)
     */
    private Message getSentMessage(EcosystemServiceIndicator esi) {
        Message sentMsg = null;
        Set<Message> msgs = esi.getMessages();
        for (Message msg : msgs) {
            if (msg != null) {
                sentMsg = msg;
                break;
            }
        }
        return sentMsg;
    }

}
