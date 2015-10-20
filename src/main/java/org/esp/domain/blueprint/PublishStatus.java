package org.esp.domain.blueprint;

/*
 * Possible state of publishing
 */

public enum PublishStatus {
    VALIDATED(1l),
    NOT_VALIDATED(2l);

    private final Long value;

    private PublishStatus(Long value){
        this.value = value;
    }
    
    public Long getValue() {
        return value;
    }


}
