package org.esp.domain.blueprint;

import it.jrc.domain.auth.Role;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(schema = "blueprint", name = "message")
public class Message {

    private Long id;

    private String text;

    private Message feedback;

    private Message parent;

    private Role author;

    private Date created;

    private EcosystemServiceIndicator ecosystemServiceIndicator;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "seq")
    @SequenceGenerator(allocationSize = 1, name = "seq", sequenceName = "blueprint.status_id_seq")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(length = 500)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @OneToOne(mappedBy = "parent")
    public Message getFeedback() {
        return feedback;
    }

    public void setFeedback(Message feedback) {
        this.feedback = feedback;
    }

    @OneToOne(cascade = CascadeType.ALL)
    public Message getParent() {
        return parent;
    }

    public void setParent(Message parent) {
        this.parent = parent;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @OneToOne
    public Role getAuthor() {
        return author;
    }

    public void setAuthor(Role author) {
        this.author = author;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public EcosystemServiceIndicator getEcosystemServiceIndicator() {
        return ecosystemServiceIndicator;
    }

    public void setEcosystemServiceIndicator(EcosystemServiceIndicator ecosystemServiceIndicator) {
        this.ecosystemServiceIndicator = ecosystemServiceIndicator;
    }

    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
        created = new Date();
    }

}
