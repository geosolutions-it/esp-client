package org.esp.domain.blueprint;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ColourMap.class)
public abstract class ColourMap_ {

    public static volatile SingularAttribute<ColourMap,String> label;

    public static volatile SingularAttribute<ColourMap,Long> id;
}
