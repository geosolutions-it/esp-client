package org.esp.server;

import it.jrc.domain.auth.Role;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.esp.domain.blueprint.EcosystemServiceIndicator;
import org.esp.domain.blueprint.Message;
import org.esp.publisher.ui.ViewModule;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class MailService {

    private String BACK_SUBJECT = "ESP-MAPPING : Send back for corrections";
    private String PUBLISH_SUBJECT = "ESP-MAPPING : Your map has been published";
    private String NEWDATASET_SUBJECT = "ESP-MAPPING : New map has been uploaded and need to be verified and published";

    private String hostName;

    private String smtpPort;

    private String username;

    private String password;

    private String from;

    private Configuration freeMarker;

    private String fromName;

    private String useSSL;

    @Inject
    public MailService(@Named("mail_hostname") String hostName,
            @Named("mail_smtpport") String smtpPort, @Named("mail_username") String username,
            @Named("mail_password") String password, @Named("mail_from") String from,
            @Named("mail_fromname") String fromName,  @Named("mailSSL") String useSSL,
            Configuration freeMarker) {
        this.hostName = hostName;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.from = from;
        this.fromName = fromName;
        this.freeMarker = freeMarker;
        this.useSSL = useSSL;
    }

    public void sendEmail(String subject, String message, String to) throws EmailException,
            IOException {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(hostName);
        email.setSmtpPort(Integer.parseInt(smtpPort));       
        Boolean ssl = Boolean.parseBoolean(useSSL);
        if(ssl){
            email.setSSLOnConnect(true);
            email.setAuthenticator(new DefaultAuthenticator(username, password));
        }
        email.setFrom(from,fromName);
        email.addTo(to);
        email.setSubject(subject);
        email.setHtmlMsg(message);
        email.setTextMsg("Your email client does not support HTML messages");
        email.send();
    }
    
    public void sendUploadedEmail(String message, String to) throws EmailException,
    	IOException {
		sendEmail(NEWDATASET_SUBJECT, message, to);
	}

    public void sendBackEmailMessage(Message message, Role from , Role to) throws EmailException, IOException,
            TemplateException {

        EcosystemServiceIndicator esi = message.getEcosystemServiceIndicator();
        Template template = this.freeMarker.getTemplate("SendBackEmail.ftl");
        Map<String, String> rootMap = new HashMap<String, String>();
        rootMap.put("SUBJECT", BACK_SUBJECT);
        rootMap.put("TO", to.getFirstName() + " " + to.getLastName());
        rootMap.put("FROM", from.getFirstName() + " " + from.getLastName()+ "( "+ from.getEmail()  +" )");
        rootMap.put("ESI_NAME", esi.toString());
        rootMap.put("ESI_LINK", ViewModule.getFullESILink(esi));
        if(message.getParent() != null){
            rootMap.put("ORIGINAL", message.getParent().getText());
        }        
        rootMap.put("MESSAGE", message.getText());
        Writer out = new StringWriter();
        template.process(rootMap, out);
        sendEmail(BACK_SUBJECT, out.toString(), to.getEmail());
    }
    
    public void sendPublishedEmailMessage(EcosystemServiceIndicator esi, Role from , Role to) throws EmailException, IOException,
    TemplateException {
        Template template = this.freeMarker.getTemplate("SendBackEmail.ftl");
        Map<String, String> rootMap = new HashMap<String, String>();
        rootMap.put("SUBJECT", PUBLISH_SUBJECT);
        rootMap.put("TO", to.getFirstName() + " " + to.getLastName());
        rootMap.put("FROM", from.getFirstName() + " " + from.getLastName()+ "( "+ from.getEmail()  +" )");
        rootMap.put("ESI_NAME", esi.toString());
        rootMap.put("ESI_LINK", ViewModule.getFullESILink(esi));   
        rootMap.put("MESSAGE", "Your map has been published");
        Writer out = new StringWriter();
        template.process(rootMap, out);
        sendEmail(PUBLISH_SUBJECT, out.toString(), to.getEmail());
    }
    
    

}
