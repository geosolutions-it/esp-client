package org.esp.domain.publisher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.vaadin.shared.ui.colorpicker.Color;

@Entity
@Table(schema = "publisher", name = "colour_map_entry")
public class ColourMapEntry {

    private Integer id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "seq")
    @SequenceGenerator(allocationSize = 1, name = "seq", sequenceName = "publisher.colour_map_entry_id_seq")
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    
    private ColourMap colourMap;

    @NotNull
    @ManyToOne
    @JoinColumn(name="colour_map_id")
    public ColourMap getColourMap() {
        return colourMap;
    }

    public void setColourMap(ColourMap colourMap) {
        this.colourMap = colourMap;
    }

    private Integer green = 0;

    @NotNull
    @Column
    public Integer getGreen() {
        return green;
    }

    public void setGreen(Integer green) {
        this.green = green;
    }

    private Integer blue = 0;

    @NotNull
    @Column
    public Integer getBlue() {
        return blue;
    }

    public void setBlue(Integer blue) {
        this.blue = blue;
    }

    private Integer red = 0;

    @NotNull
    @Column
    public Integer getRed() {
        return red;
    }

    public void setRed(Integer red) {
        this.red = red;
    }

    private Integer alpha = 0;

    @NotNull
    @Column
    public Integer getAlpha() {
        return alpha;
    }

    public void setAlpha(Integer alpha) {
        this.alpha = alpha;
    }
    
    private Double value;
    
    @Transient
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    private String label;

    @NotNull
    @Column
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    @Transient
    public Color getColor() {
        return new Color(red, green, blue, alpha);
    }
    
    public void setColor(Color color) {
        red = color.getRed();
        green = color.getGreen();
        blue = color.getBlue();
        alpha = color.getAlpha();
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
    
        if (obj instanceof ColourMapEntry) {
            ColourMapEntry comparee = (ColourMapEntry) obj;
            if (getId() == null) {
                return false;
            }
            if (comparee.getId().equals(getId())) {
                return true;
            }
        }
        return super.equals(obj);
    }
    
}
