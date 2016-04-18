package org.esp.domain.blueprint;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(schema = "blueprint", name = "mmu_uom")
public class MappingUnit {

    private Long id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "seq")
    @SequenceGenerator(allocationSize = 1, name = "seq", sequenceName = "blueprint.mmu_uom_id_seq")
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private String uom;

    @Column
    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    @Override
    public String toString() {
        return uom;
    }
    

    @Override
    public int hashCode() {
        if (id != null) {
            return id.intValue();
        }
        return super.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
    
        if (obj instanceof MappingUnit) {
        	MappingUnit otherObj = (MappingUnit) obj;
            if (otherObj.getId().equals(this.getId())) {
                return true;
            }
            return false;
        }
        return super.equals(obj);
    }
}
