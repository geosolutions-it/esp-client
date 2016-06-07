package org.esp.publisher.ui;

import it.geosolutions.geonetwork.GNClient;
import it.geosolutions.geonetwork.exception.GNLibException;
import it.geosolutions.geonetwork.exception.GNServerException;
import it.geosolutions.geonetwork.util.GNInsertConfiguration;
import it.geosolutions.geonetwork.util.GNPriv;
import it.geosolutions.geonetwork.util.GNPrivConfiguration;
import it.geosolutions.geoserver.rest.decoder.RESTBoundingBox;
import it.geosolutions.geoserver.rest.decoder.RESTCoverage;
import it.geosolutions.geoserver.rest.decoder.RESTFeatureType;
import it.jrc.auth.RoleManager;
import it.jrc.domain.auth.Role;
import it.jrc.form.editor.EntityTable;
import it.jrc.form.filter.FilterPanel;
import it.jrc.form.view.TwinPanelView;
import it.jrc.persist.ContainerManager;
import it.jrc.persist.Dao;
import it.jrc.ui.HtmlLabel;
import it.jrc.ui.SimplePanel;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.esp.domain.blueprint.Biome;
import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.EcosystemServiceIndicator_;
import org.esp.domain.blueprint.Message;
import org.esp.domain.blueprint.PublishStatus;
import org.esp.domain.blueprint.Status;
import org.esp.publisher.GeoserverRestApi;
import org.esp.publisher.LayerManager;
import org.esp.server.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addon.leaflet.LMap;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.vaadin.addon.jpacontainer.EntityItem;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerItem;
import com.vaadin.data.Container.Filterable;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.Compare.Equal;
import com.vaadin.data.util.filter.Not;
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
import com.vaadin.ui.Notification;
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

    private MailService mailService;
    
	private String geonetworkUrl;
	private String geonetworkUser;
	private String geonetworkPassword;
	
	private String baseUrl;
	private String geoserverUrl;

	
	DateFormat inspireDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	
	GeoserverRestApi gsr;

    private Logger logger = LoggerFactory.getLogger(SearchView.class);
    
    @Inject
    public SearchView(final Dao dao, RoleManager roleManager_, MailService mailService_, 
			final ModalMessageWindow modalMessageWindow,
            @Named("gs_wms_url") String defaultWms,
            GeoserverRestApi gsr,
            @Named("gn_url") String geonetworkUrl,
			@Named("gn_user") String geonetworkUser,
			@Named("gn_password") String geonetworkPassword,
            @Named("login_page_url") String loginPageUrl) {

        ContainerManager<EcosystemServiceIndicator> containerManager = new ContainerManager<EcosystemServiceIndicator>(
                dao, EcosystemServiceIndicator.class);
        this.mailService = mailService_;
        this.modalMessageWindow = modalMessageWindow;

		this.geonetworkUrl = geonetworkUrl;
		this.geonetworkUser = geonetworkUser;
		this.geonetworkPassword = geonetworkPassword;
		this.gsr = gsr;
		this.geoserverUrl = defaultWms;
		this.baseUrl = loginPageUrl.substring(0, loginPageUrl.lastIndexOf("/"));
		
        this.modalMessageWindow.addCloseListener(new Window.CloseListener() {
            @Override
            public void windowClose(CloseEvent e) {
                table.refreshRowCache();
                table.refresh();
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
        this.roleManager = roleManager_;
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
                
                pb = new Button("Publish");
                pb.setEnabled(false);
                pb.addClickListener(new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        Iterator<JPAContainerItem<?>> i = checkedItems.iterator();
                        while (i.hasNext()) {
                            JPAContainerItem<?> item = i.next();
                            item.getItemProperty(EcosystemServiceIndicator_.status.getName())
                                    .setValue(validated);
                            /*
                             * Send mail to users
                             */
                            EcosystemServiceIndicator esi = (EcosystemServiceIndicator) item.getEntity();
        					try {
        						EcosystemServiceIndicator persistedEntity = dao.find(EcosystemServiceIndicator.class, esi.getId());
								long id = publishOnGeoNetwork(persistedEntity);
								if(id != -1) {
									persistedEntity.setGeonetworkMetadataId(id);
									dao.persist(persistedEntity);
								}
        						
        					} catch (IOException e) {
        						logger.error(e.getMessage(),e);
                                Notification.show("Error creating metadata XML for GeoNetwork publishing : " + e.getMessage(),
                                        Notification.Type.ERROR_MESSAGE);
        					} catch (GNLibException e) {
        						logger.error(e.getMessage(),e);
                                Notification.show("Error publishing metadata on GeoNetwork : " + e.getMessage(),
                                        Notification.Type.ERROR_MESSAGE);
        					} catch (GNServerException e) {
        						logger.error(e.getMessage(),e);
                                Notification.show("Error connecting to GeoNetwork for metadata publishing: " + e.getMessage(),
                                        Notification.Type.ERROR_MESSAGE);
        					}
                            try {
                                mailService.sendPublishedEmailMessage(esi, roleManager.getRole(), esi.getRole());
                            } catch (Exception e) {
                                logger.error(e.getMessage(),e);
                                Notification.show("Publish problem: unable to send email to : " + esi.getRole().getEmail() + "\n for " + esi.toString(),
                                        Notification.Type.ERROR_MESSAGE);
                            }
                        }
                        esiContainer.refresh();
                        resetChecked();
                    }
                });
                buttonBar.addComponent(pb);
                buttonBar.setComponentAlignment(pb, Alignment.MIDDLE_CENTER);
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

    protected long publishOnGeoNetwork(EcosystemServiceIndicator entity) throws IOException, GNLibException, GNServerException {
		RESTBoundingBox bbox = null;
		if (entity.getSpatialDataType().getId() == 1) {
			RESTCoverage layerInfo = gsr.getCoverageInfo(entity.getLayerName());
			bbox = layerInfo.getLatLonBoundingBox();
		} else {
			RESTFeatureType layerInfo = gsr.getLayerInfo(entity.getLayerName());
			bbox = layerInfo.getLatLonBoundingBox();
		}

		String thesaurusTemplate = StringUtils.join(
				IOUtils.readLines(getClass().getResourceAsStream(
						"/thesaurus_keywords.xml")), "\n");
		String resourceTemplate = StringUtils.join(
				IOUtils.readLines(getClass().getResourceAsStream(
						"/resource_template.xml")), "\n");
		StringBuilder builder = new StringBuilder();

		for (String line : IOUtils.readLines(getClass().getResourceAsStream(
				"/publish_gn_template.xml"))) {
			builder.append(parseGeonetworkTemplate(
					entity,
					line,
					thesaurusTemplate,
					resourceTemplate,
					new double[] { bbox.getMinX(), bbox.getMaxX(),
							bbox.getMinY(), bbox.getMaxY() }));
		}
		GNClient client = new GNClient(geonetworkUrl, geonetworkUser, geonetworkPassword);
		
		GNInsertConfiguration cfg = new GNInsertConfiguration();
        cfg.setCategory("datasets");
        cfg.setGroup("1"); // group 1 is usually "all"
        cfg.setStyleSheet("_none_");
        cfg.setValidate(Boolean.FALSE);
        cfg.setEncoding("UTF-8");
		
        GNPrivConfiguration pcfg = new GNPrivConfiguration();

        pcfg.addPrivileges(GNPrivConfiguration.GROUP_GUEST,    EnumSet.of(GNPriv.FEATURED));
        pcfg.addPrivileges(GNPrivConfiguration.GROUP_INTRANET, EnumSet.of(GNPriv.DYNAMIC, GNPriv.FEATURED));
        pcfg.addPrivileges(GNPrivConfiguration.GROUP_ALL,      EnumSet.of(GNPriv.VIEW, GNPriv.DYNAMIC, GNPriv.FEATURED));
        pcfg.addPrivileges(2, EnumSet.allOf(GNPriv.class));
        
        
        File tempFile = File.createTempFile("geonetwork", "xml");
        try {
	        FileUtils.write(tempFile, builder.toString(), "UTF-8");
	        if(entity.getGeonetworkMetadataId() != null) {
	        	client.updateMetadata(entity.getGeonetworkMetadataId(), tempFile, "UTF-8");
	        	return -1;
	        } else {
	        	return client.insertMetadata(cfg, tempFile);
	        }
	        
        } finally {
        	tempFile.delete();
        }
	}
    
    private String parseGeonetworkTemplate(EcosystemServiceIndicator entity,
			String line, String thesaurusTemplate, String resourceTemplate,
			double[] bbox) {
		Calendar startYear = Calendar.getInstance();
		if (entity.getInspireStartYear() != null) {
			startYear.set(entity.getInspireStartYear(), 1, 1, 0, 0, 0);
		}
		Calendar endYear = Calendar.getInstance();
		if (entity.getInspireEndYear() != null) {
			endYear.set(entity.getInspireEndYear(), 1, 1, 0, 0, 0);
		}
		return line
				.replace("{{FILEIDENTIFIER}}", entity.getId() + "")
				.replace("{{LANGUAGE}}", entity.getInspireLanguage())
				.replace("{{LANGUAGELOWER}}", entity.getInspireLanguage().toLowerCase())
				.replace("{{DATESTAMP}}",
						inspireDateFormat.format(entity.getDateCreated()))
				.replace("{{DATADATE}}",
						inspireDateFormat.format(entity.getDateCreated()))
				.replace("{{CRS}}", entity.getInspireCrs())
				.replace("{{TITLE}}",
						normalizeTemplateVariable(entity.getInspireTitle()))
				.replace("{{ABSTRACT}}",
						normalizeTemplateVariable(entity.getInspireAbstract()))
				.replace(
						"{{PURPOSE}}",
						entity.getStudy().getStudyPurpose() != null ? entity
								.getStudy().getStudyPurpose().getLabel() : "")
				.replace(
						"{{CREDIT}}",
						normalizeTemplateVariable(entity.getStudy()
								.getFundingSource())
								+ ", "
								+ normalizeTemplateVariable(entity.getStudy()
										.getMainInvestigators()))
				.replace(
						"{{DATAPOCORG}}",
						normalizeTemplateVariable(entity
								.getInspireOwnerOrganization()))
				.replace("{{DATAPOCPOS}}",
						normalizeTemplateVariable(entity.getInspireOwnerName()))
				.replace(
						"{{DATAPOCMAIL}}",
						normalizeTemplateVariable(entity.getInspireOwnerEmail()))
				.replace(
						"{{DATAPOCSITE}}",
						normalizeTemplateVariable(entity.getInspireOwnerSite()))	
				.replace(
						"{{KEYWORD}}",
						normalizeTemplateVariable(entity.getStudy()
								.getKeywords()))
				.replace("{{THESAURUSKEYWORDS}}",
						getThesaurusKeywords(entity, thesaurusTemplate))
				.replace("{{TOPIC}}",
						entity.getInspireTopicCategory() != null ? entity.getInspireTopicCategory().getCategory() : "")
				.replace(
						"{{CONSTRAINTS}}",
						normalizeTemplateVariable(entity
								.getInspireResourceConstraints()))
				.replace(
						"{{SPATIALREPRESENTATION}}",
						entity.getSpatialDataType().getId() == 1 ? "grid"
								: "vector")
				.replace(
						"{{TEMPORALSTART}}",
						entity.getInspireStartYear() != null ? inspireDateFormat
								.format(startYear.getTime()) : "")
				.replace(
						"{{TEMPORALEND}}",
						entity.getInspireStartYear() != null ? inspireDateFormat
								.format(endYear.getTime()) : "")
				.replace("{{EXTENT}}",
						bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3])
				.replace("{{WEST}}", bbox[0] + "")
				.replace("{{EAST}}", bbox[1] + "")
				.replace("{{SOUTH}}", bbox[2] + "")
				.replace("{{NORTH}}", bbox[3] + "")
				.replace(
						"{{SUPPLEMENTAL}}",
						normalizeTemplateVariable(entity.getStudy()
								.getProjectReferences()))
				.replace(
						"{{LINEAGESTATEMENT}}",
						entity.getQuantificationMethod() != null ? entity
								.getQuantificationMethod().getLabel() : "")
				.replace("{{RESOURCES}}",
						getResources(entity, resourceTemplate))
				+ "\n";
	}

	private CharSequence getResources(EcosystemServiceIndicator entity,
			String resourceTemplate) {
		StringBuilder builder = new StringBuilder();

		String downloadUrl = baseUrl + "/getoriginal/" + entity.getId() + "/" + entity.getSpatialDataType().getId();
		builder.append(createResource(resourceTemplate,
				"WWW:LINK-1.0-http--link", downloadUrl , "<gco:CharacterString/>"));
		if (entity.getSpatialDataType().getId() == 1) {
			builder.append(createResource(resourceTemplate, "FILE:RASTER",
					downloadUrl,
					"<gmx:MimeFileType xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" type=\"\"/>"));
		}
		builder.append(createResource(resourceTemplate,
				"OGC:WMS-1.1.1-http-get-map", geoserverUrl, "<gco:CharacterString>" + entity.getLayerName() + "</gco:CharacterString>"));

		return builder.toString() + "\n";
	}

	private String createResource(String template, String protocol, String url,
			String name) {
		return template.replace("{{RESOURCEURL}}", url)
				.replace("{{RESOURCENAME}}", name)
				.replace("{{RESOURCEPROTOCOL}}", protocol);
	}

	private String normalizeTemplateVariable(String value) {
		return value != null ? value : "";
	}

	private String getThesaurusKeywords(EcosystemServiceIndicator entity,
			String thesaurusTemplate) {
		StringBuilder builder = new StringBuilder();

		builder.append(createThesaurusKeyword(thesaurusTemplate,
				"JRC Ecosystem Service", entity.getEcosystemService()
						.getDescription(), entity.getDateCreated()));
		builder.append(createThesaurusKeyword(thesaurusTemplate,
				"JRC Indicator", entity.getIndicator().getLabel(),
				entity.getDateCreated()));
		builder.append(createThesaurusKeyword(thesaurusTemplate,
				"JRC Project Type",
				entity.getStudy().getProjectType() != null ? entity.getStudy()
						.getProjectType().getLabel() : "", entity
						.getDateCreated()));
		builder.append(createThesaurusKeyword(thesaurusTemplate,
				"JRC Ecosystem Service Accounting Type", entity
						.getEcosystemServiceAccountingType() != null ? entity
						.getEcosystemServiceAccountingType().getLabel() : "",
				entity.getDateCreated()));
		builder.append(createThesaurusKeyword(thesaurusTemplate,
				"JRC Ecosystem Service Benefit Type", entity
						.getEcosystemServiceBenefitType() != null ? entity
						.getEcosystemServiceBenefitType().getLabel() : "",
				entity.getDateCreated()));
		builder.append(createThesaurusKeyword(thesaurusTemplate,
				"JRC Spatial Level", entity.getSpatialLevel() != null ? entity
						.getSpatialLevel().getLabel() : "", entity
						.getDateCreated()));
		if (entity.getBiomes() != null) {
			for (Biome biome : entity.getBiomes()) {
				builder.append(createThesaurusKeyword(thesaurusTemplate,
						"JRC Biomes", biome.getLabel(), entity.getDateCreated()));
			}
		}
		builder.append(createThesaurusKeyword(thesaurusTemplate,
				"JRC Objectives Met",
				entity.getStudyObjectiveMet() != null ? entity
						.getStudyObjectiveMet().getLabel() : "", entity
						.getDateCreated()));
		if(entity.getInspireTheme() != null) {
			builder.append(createThesaurusKeyword(thesaurusTemplate, "GEMET", entity.getInspireTheme().getTheme(), entity.getDateCreated()));
		}
		return builder.toString() + "\n";
	}

	private Object createThesaurusKeyword(String template, String key,
			String value, Date date) {
		return template.replace("{{THESAURUSKEYWORD}}", value)
				.replace("{{THESAURUSNAME}}", key)
				.replace("{{THESAURUSDATE}}", inspireDateFormat.format(date));
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
        
        fp.addFilterField(EcosystemServiceIndicator_.ecosystemService, "SELECT es FROM EcosystemService es WHERE es.id > 0");
        
        fp.addFilterField(EcosystemServiceIndicator_.study, "SELECT st FROM Study st WHERE st.id > 0");
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
    	if(event.getParameters() != null && event.getParameters().equals("reset")) {
    		entitySelected(null);
    		layerManager.setSurfaceLayerName("", 0);
    		mapLegend.setValue(null);
    	} else if (selectedEntity == null) {

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
        /*
         * Generic user see: 
         * - own not temporary (validated or not)
         * - all validated
         */
        if (!roleManager.getRole().getIsSuperUser()) {
            toFilter.addContainerFilter(
                    new Or(
                            new And(
                                    new Not(new Equal(EcosystemServiceIndicator_.status.getName(), PublishStatus.TEMPORARY.getValue())), 
                                    new Equal(EcosystemServiceIndicator_.role.getName(), roleManager.getRole())),
                            new Equal(EcosystemServiceIndicator_.status.getName(), PublishStatus.VALIDATED.getValue())
                    )
            );
        /*
         * Super user see: 
         * - all not temporary 
         */
        }else{           
            toFilter.addContainerFilter(new Not(new Equal(EcosystemServiceIndicator_.status.getName(), PublishStatus.TEMPORARY.getValue())));
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
    	if(sb != null) {
    		sb.setEnabled(false);
    	}
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
