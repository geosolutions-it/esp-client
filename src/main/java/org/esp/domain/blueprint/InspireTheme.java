package org.esp.domain.blueprint;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


@Entity
@Table(schema = "blueprint", name = "inspire_theme")
public class InspireTheme {

    private Long id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "seq")
    @SequenceGenerator(allocationSize = 1, name = "seq", sequenceName = "blueprint.inspire_theme_id_seq")
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private String theme;

    @Column
    @NotNull
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    
    @Override
    public String toString() {
        return theme;
    }
    
    @Override
    public boolean equals(Object obj) {
        
        if (obj instanceof InspireTheme) {
            InspireTheme comparee = (InspireTheme) obj;
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
