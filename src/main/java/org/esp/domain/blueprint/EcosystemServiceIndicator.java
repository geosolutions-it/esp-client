package org.esp.domain.blueprint;

import it.jrc.domain.adminunits.Grouping;
import it.jrc.domain.auth.HasRole;
import it.jrc.domain.auth.Role;

import java.io.File;
import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.esp.domain.publisher.ColourMap;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Polygon;

@Entity
@Table(schema = "blueprint", name = "ecosystem_service_indicator")
public class EcosystemServiceIndicator implements HasRole {

    private Long id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "seq")
    @SequenceGenerator(allocationSize = 1, name = "seq", sequenceName = "blueprint.ecosystem_service_indicator_id_seq")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private EcosystemService ecosystemService;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "ecosystem_service_id")
    public EcosystemService getEcosystemService() {
        return ecosystemService;
    }

    public void setEcosystemService(EcosystemService ecosystemService) {
        this.ecosystemService = ecosystemService;
    }

    private Study study;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "study_id")
    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    private StudyObjectiveMet studyObjectiveMet;

    @ManyToOne
    @JoinColumn(name = "study_objective_met_id")
    public StudyObjectiveMet getStudyObjectiveMet() {
        return studyObjectiveMet;
    }

    public void setStudyObjectiveMet(StudyObjectiveMet studyObjectiveMet) {
        this.studyObjectiveMet = studyObjectiveMet;
    }

    private SpatialLevel spatialLevel;

    @ManyToOne
    @JoinColumn(name = "spatial_level_id")
    public SpatialLevel getSpatialLevel() {
        return spatialLevel;
    }

    public void setSpatialLevel(SpatialLevel spatialLevel) {
        this.spatialLevel = spatialLevel;
    }

    private QuantificationUnit quantificationUnit;

    @ManyToOne
    @JoinColumn(name = "quantification_unit_id")
    public QuantificationUnit getQuantificationUnit() {
        return quantificationUnit;
    }

    public void setQuantificationUnit(QuantificationUnit quantificationUnit) {
        this.quantificationUnit = quantificationUnit;
    }
    
    private MappingUnit mappingUnit;
    
    @ManyToOne
    @JoinColumn(name = "fk_mmu_uom")
    public MappingUnit getMinimumMappingUnitUom() {
    	return mappingUnit;
    }
    
    public void setMinimumMappingUnitUom(MappingUnit mappingUnit) {
    	this.mappingUnit = mappingUnit;
    }

    private ArealUnit arealUnit;

    @ManyToOne
    @JoinColumn(name = "areal_unit_id")
    public ArealUnit getArealUnit() {
        return arealUnit;
    }

    public void setArealUnit(ArealUnit arealUnit) {
        this.arealUnit = arealUnit;
    }

    private Indicator indicator;

    @NotNull
    @ManyToOne
    public Indicator getIndicator() {
        return indicator;
    }

    public void setIndicator(Indicator indicator) {
        this.indicator = indicator;
    }

    private TemporalUnit temporalUnit;

    @ManyToOne
    @JoinColumn(name = "temporal_unit_id")
    public TemporalUnit getTemporalUnit() {
        return temporalUnit;
    }

    public void setTemporalUnit(TemporalUnit temporalUnit) {
        this.temporalUnit = temporalUnit;
    }

    private QuantificationMethod quantificationMethod;

    @ManyToOne
    @JoinColumn(name = "quantification_method_id")
    public QuantificationMethod getQuantificationMethod() {
        return quantificationMethod;
    }

    public void setQuantificationMethod(
            QuantificationMethod quantificationMethod) {
        this.quantificationMethod = quantificationMethod;
    }

    private EcosystemServiceAccountingType ecosystemServiceAccountingType;

    @ManyToOne
    @JoinColumn(name = "ecosystem_service_accounting_type_id")
    public EcosystemServiceAccountingType getEcosystemServiceAccountingType() {
        return ecosystemServiceAccountingType;
    }

    public void setEcosystemServiceAccountingType(
            EcosystemServiceAccountingType ecosystemServiceAccountingType) {
        this.ecosystemServiceAccountingType = ecosystemServiceAccountingType;
    }

    private EcosystemServiceBenefitType ecosystemServiceBenefitType;

    @ManyToOne
    @JoinColumn(name = "ecosystem_service_benefit_type_id")
    public EcosystemServiceBenefitType getEcosystemServiceBenefitType() {
        return ecosystemServiceBenefitType;
    }

    public void setEcosystemServiceBenefitType(
            EcosystemServiceBenefitType ecosystemServiceBenefitType) {
        this.ecosystemServiceBenefitType = ecosystemServiceBenefitType;
    }

    private String minimumMappingUnit;

    @Column(name = "minimum_mapping_unit")
    public String getMinimumMappingUnit() {
        return minimumMappingUnit;
    }

    public void setMinimumMappingUnit(String minimumMappingUnit) {
        this.minimumMappingUnit = minimumMappingUnit;
    }

    private String spatialResolution;

    @Column(name = "spatial_resolution")
    public String getSpatialResolution() {
        return spatialResolution;
    }

    public void setSpatialResolution(String spatialResolution) {
        this.spatialResolution = spatialResolution;
    }

    private Integer endYear;

    @Column(name = "end_year")
    public Integer getEndYear() {
        return endYear;
    }

    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    private String comments;

    @Column
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    private Integer startYear;

    @Column(name = "start_year")
    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    private Set<Biome> biomes;

    @ManyToMany
    @JoinTable(name = "blueprint.ecosystem_service_indicator_biome", joinColumns = @JoinColumn(name = "ecosystem_service_indicator_id"), inverseJoinColumns = @JoinColumn(name = "biome_id"))
    public Set<Biome> getBiomes() {
        return biomes;
    }

    public void setBiomes(Set<Biome> biomes) {
        this.biomes = biomes;
    }

    private Set<Grouping> regions;

    @ManyToMany
    @JoinTable(name = "blueprint.ecosystem_service_indicator_grouping", joinColumns = @JoinColumn(name = "ecosystem_service_indicator_id"), inverseJoinColumns = @JoinColumn(name = "grouping_id"))
    public Set<Grouping> getRegions() {
        return regions;
    }

    public void setRegions(Set<Grouping> groupings) {
        this.regions = groupings;
    }

    private Set<DataSource> dataSources;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "blueprint.ecosystem_service_indicator_data_source", joinColumns = @JoinColumn(name = "ecosystem_service_indicator_id"), inverseJoinColumns = @JoinColumn(name = "data_source_id"))
    public Set<DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(Set<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    private SpatialDataType spatialDataType;

    @NotNull
    @ManyToOne
    @JoinColumn(name="spatial_data_type_id")
    public SpatialDataType getSpatialDataType() {
        return spatialDataType;
    }

    public void setSpatialDataType(SpatialDataType spatialDataType) {
        this.spatialDataType = spatialDataType;
    }

    private Double maxVal;

    @Column(name="max_val")
    public Double getMaxVal() {
        return maxVal;
    }

    public void setMaxVal(Double maxVal) {
        this.maxVal = maxVal;
    }

    private Polygon envelope;

    @Column(name="envelope_")
    @Type(type = "org.hibernate.spatial.GeometryType")
    public Polygon getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Polygon envelope) {
        this.envelope = envelope;
    }

    private String layerName;

    @Column(name="layer_name")
    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }
    
    private String attributeName;
    
    @Column(name="attribute_name")
    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    private String srid;

    @Column
    public String getSrid() {
        return srid;
    }

    public void setSrid(String srid) {
        this.srid = srid;
    }

    private Double pixelSizeY;

    @Column(name="pixel_size_y")
    public Double getPixelSizeY() {
        return pixelSizeY;
    }

    public void setPixelSizeY(Double pixelSizeY) {
        this.pixelSizeY = pixelSizeY;
    }

    private Double pixelSizeX;

    @Column(name="pixel_size_x")
    public Double getPixelSizeX() {
        return pixelSizeX;
    }

    public void setPixelSizeX(Double pixelSizeX) {
        this.pixelSizeX = pixelSizeX;
    }

    private Double minVal;

    @Column(name="min_val")
    public Double getMinVal() {
        return minVal;
    }

    public void setMinVal(Double minVal) {
        this.minVal = minVal;
    }
    
    private Integer intervalsNumber;

    @Column(name="intervals_num")
    public Integer getIntervalsNumber() {
        return intervalsNumber;
    }

    public void setIntervalsNumber(Integer intervalsNumber) {
        this.intervalsNumber = intervalsNumber;
    }
    
    private ColourMap colourMap;

    @ManyToOne
    @JoinColumn(name = "colour_map_id")
    public ColourMap getColourMap() {
        return colourMap;
    }

    public void setColourMap(ColourMap colourMap) {
        this.colourMap = colourMap;
    }
    
    private Classification classification;

    @ManyToOne
    @JoinColumn(name = "classification_id")
    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }
    
    private Status status;
    
    @ManyToOne
    @JoinColumn(name = "status_id")
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    private String spatialReferenceInfo;
    
    @Column(name = "spatial_reference_info")
    public String getSpatialReferenceInfo() {
        return spatialReferenceInfo;
    }
    
    public void setSpatialReferenceInfo(String spatialReferenceInfo) {
        this.spatialReferenceInfo = spatialReferenceInfo;
    }
    
    private Long geonetworkMetadataId;
    
    @Column(name = "gn_metadata_id")
    public Long getGeonetworkMetadataId() {
        return geonetworkMetadataId;
    }
    
    public void setGeonetworkMetadataId(Long geonetworkMetadataId) {
        this.geonetworkMetadataId = geonetworkMetadataId;
    }
    

    private Role role;
    
    @ManyToOne
    @JoinColumn(name = "role_id")
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    private Date dateCreated;
    
    @Column(name = "date_created")
    @Generated(value = GenerationTime.INSERT)
    public Date getDateCreated() {
        return dateCreated;
    }
    
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    private Date dateUpdated;
    
    @Column(name = "date_updated")
    //@Generated(value = GenerationTime.ALWAYS)
    public Date getDateUpdated() {
        return dateUpdated;
    }
    
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    private Set<Message> messages;

    @OneToMany(cascade = CascadeType.ALL, mappedBy="ecosystemServiceIndicator")
    public Set<Message> getMessages() {
        return messages;
    }

    public void setMessages(Set<Message> messages) {
        this.messages = messages;
    }
    
    private File file;
    
    @Transient
    public File getFile() {
        return file;
    }
    
    public void setFile(File file) {
        this.file = file;
    }
    
    @Transient
    public long getTimestamp() {
        if(getDateUpdated() != null) {
            return getDateUpdated().getTime();
        }
        if(getDateCreated() != null) {
            return getDateCreated().getTime();
        }
        return 0;
    }
    
    private String inspireTitle;

    @Column(name = "inspire_title")
    public String getInspireTitle() {
        return inspireTitle;
    }

    public void setInspireTitle(String inspireTitle) {
        this.inspireTitle = inspireTitle;
    }
    
    private String inspireAbstract;

    @Column(name = "inspire_abstract")
    public String getInspireAbstract() {
        return inspireAbstract;
    }

    public void setInspireAbstract(String inspireAbstract) {
        this.inspireAbstract = inspireAbstract;
    }
    
    private InspireTopicCategory inspireTopicCategory;

    @ManyToOne
    @JoinColumn(name = "inspire_topic_category")
    public InspireTopicCategory getInspireTopicCategory() {
        return inspireTopicCategory;
    }

    public void setInspireTopicCategory(InspireTopicCategory inspireTopicCategory) {
        this.inspireTopicCategory = inspireTopicCategory;
    }
    
    private InspireTheme inspireTheme;

    @ManyToOne
    @JoinColumn(name = "inspire_theme")
    public InspireTheme getInspireTheme() {
        return inspireTheme;
    }

    public void setInspireTheme(InspireTheme inspireTheme) {
        this.inspireTheme = inspireTheme;
    }
    
    private String inspireResourceConstraints;


    @Column(name = "inspire_resource_constraints")
    public String getInspireResourceConstraints() {
        return inspireResourceConstraints;
    }

    public void setInspireResourceConstraints(String inspireResourceConstraints) {
        this.inspireResourceConstraints = inspireResourceConstraints;
    }
    
    private String inspireLanguage;

    @Column(name = "inspire_language")
    public String getInspireLanguage() {
        return inspireLanguage;
    }

    public void setInspireLanguage(String inspireLanguage) {
        this.inspireLanguage = inspireLanguage;
    }
    
    private Integer inspireStartYear;

    @Column(name = "inspire_start_year")
    public Integer getInspireStartYear() {
        return inspireStartYear;
    }

    public void setInspireStartYear(Integer inspireStartYear) {
        this.inspireStartYear = inspireStartYear;
    }
    
    private Integer inspireEndYear;

    @Column(name = "inspire_end_year")
    public Integer getInspireEndYear() {
        return inspireEndYear;
    }

    public void setInspireEndYear(Integer inspireEndYear) {
        this.inspireEndYear = inspireEndYear;
    }
    
    private String inspireLineage;

    @Column(name = "inspire_lineage")
    public String getInspireLineage() {
        return inspireLineage;
    }

    public void setInspireLineage(String inspireLineage) {
        this.inspireLineage = inspireLineage;
    }
    
    private String inspireCrs;

    @Column(name = "inspire_crs")
    public String getInspireCrs() {
        return inspireCrs;
    }

    public void setInspireCrs(String inspireCrs) {
        this.inspireCrs = inspireCrs;
    }
    
    private String inspireOwnerName;
    
    private String inspireOwnerOrganization;
    
    private String inspireOwnerEmail;
    
    private String inspireOwnerSite;
    
    
    @Column(name = "inspire_owner_name")
    public String getInspireOwnerName() {
		return inspireOwnerName;
	}

	public void setInspireOwnerName(String inspireOwnerName) {
		this.inspireOwnerName = inspireOwnerName;
	}

	@Column(name = "inspire_owner_organization")
	public String getInspireOwnerOrganization() {
		return inspireOwnerOrganization;
	}

	public void setInspireOwnerOrganization(String inspireOwnerOrganization) {
		this.inspireOwnerOrganization = inspireOwnerOrganization;
	}

	@Column(name = "inspire_owner_email")
	public String getInspireOwnerEmail() {
		return inspireOwnerEmail;
	}

	public void setInspireOwnerEmail(String inspireOwnerEmail) {
		this.inspireOwnerEmail = inspireOwnerEmail;
	}

	@Column(name = "inspire_owner_site")
	public String getInspireOwnerSite() {
		return inspireOwnerSite;
	}

	public void setInspireOwnerSite(String inspireOwnerSite) {
		this.inspireOwnerSite = inspireOwnerSite;
	}

	@Override
    public String toString() {
        return String.format("%s - %s", ecosystemService, indicator);
    }

    @Override
    public boolean equals(Object obj) {
    
        if (obj instanceof EcosystemServiceIndicator) {
            EcosystemServiceIndicator comparee = (EcosystemServiceIndicator) obj;
            if (comparee.getId().equals(getId())) {
                return true;
            }
            return false;
        }
        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        if (id != null) {
            return id.intValue();
        }
        return super.hashCode();
    }

}