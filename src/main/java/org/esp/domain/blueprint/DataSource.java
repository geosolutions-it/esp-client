package org.esp.domain.blueprint;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.JoinTable;
import javax.persistence.CascadeType;
import java.util.Set;

@Entity
@Table(schema = "blueprint", name = "data_source")
public class DataSource {

    private Long id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "seq")
    @SequenceGenerator(allocationSize = 1, name = "seq", sequenceName = "blueprint.data_source_id_seq")
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private String label;

    @Column(name="label")
    public String getName() {
        return label;
    }

    public void setName(String label) {
        this.label = label;
    }

    private String url;

    @Column
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private Set<EcosystemServiceIndicator> ecosystemServiceIndicators;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "blueprint.ecosystem_service_indicator_data_source", joinColumns = @JoinColumn(name = "data_source_id"), inverseJoinColumns = @JoinColumn(name = "ecosystem_service_indicator_id"))
    public Set<EcosystemServiceIndicator> getEcosystemServiceIndicators() {
        return ecosystemServiceIndicators;
    }

    public void setEcosystemServiceIndicators(Set<EcosystemServiceIndicator> ecosystemServiceIndicators) {
        this.ecosystemServiceIndicators = ecosystemServiceIndicators;
    }

    @Override
    public String toString() {
        return label;
    }
    
    @Override
    public boolean equals(Object obj) {
    
        if (obj instanceof DataSource) {
            DataSource comparee = (DataSource) obj;
            if (comparee.getId().equals(getId())) {
                return true;
            }
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return id.intValue();
    }
    
}
