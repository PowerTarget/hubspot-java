package com.integrationagent.hubspotApi.domain;

import com.google.common.base.Strings;
import com.integrationagent.hubspotApi.service.HttpService;
import com.integrationagent.hubspotApi.utils.HubSpotException;
import com.integrationagent.hubspotApi.utils.HubSpotHelper;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HSContact {

    private Map<String, String> properties = new HashMap<>();

    public HSContact() {
    }

    public HSContact(String email, String firstname, String lastname) {
        this.properties.put("email", email);
        this.properties.put("firstname", firstname);
        this.properties.put("lastname", lastname);
    }

    public HSContact(long id, String email, String firstname, String lastname, Map<String, String> properties) {
        this.properties.put("vid", Long.toString(id));
        this.properties.put("email", email);
        this.properties.put("firstname", firstname);
        this.properties.put("lastname", lastname);
        this.properties.putAll(properties);
    }

    public long getId() {
        return !Strings.isNullOrEmpty(this.properties.get("vid")) ? Long.parseLong(this.properties.get("vid")) : 0;
    }

    public HSContact setId(long id) {
        this.properties.put("vid", Long.toString(id));
        return this;
    }

    public String getEmail() {
        return this.properties.get("email");
    }

    public HSContact setEmail(String email) {
        this.properties.put("email", email);
        return this;
    }

    public String getFirstname() {
        return this.properties.get("firstname");
    }

    public HSContact setFirstname(String firstname) {
        this.properties.put("firstname", firstname);
        return this;
    }

    public String getLastname() {
        return this.properties.get("lastname");
    }

    public HSContact setLastname(String lastname) {
        this.properties.put("lastname", lastname);
        return this;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public HSContact setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public HSContact setProperty(String property, String value) {
        this.properties.put(property, value);
        return this;
    }

    public String getProperty(String property) {
        return this.properties.get(property);
    }

    public String toJsonString() {
        Map<String, String> properties = this.properties.entrySet().stream()
                                                        .filter(p -> !p.getKey().equals("vid"))
                                                        .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        return HubSpotHelper.mapToJsonString(properties);
    }

    public static HSContact retrieveByEmail(String email) throws HubSpotException {
        String url = "/contacts/v1/contact/email/" + email + "/profile";
        return getContact(url);
    }

    public static HSContact retrieveById(long id) throws HubSpotException {
        String url = "/contacts/v1/contact/vid/" + id + "/profile";
        return getContact(url);
    }

    private static HSContact getContact(String url) throws HubSpotException {
        JsonNode jsonBody = HttpService.getRequest(url);
        return parseContactData(jsonBody);
    }

    public static HSContact create(HSContact contact) throws HubSpotException {
        if (Strings.isNullOrEmpty(contact.getEmail())) {
            throw new HubSpotException("User email must be provided");
        }

        String url = "/contacts/v1/contact";
        String properties = contact.toJsonString();

        try {
            JsonNode jsonBody = HttpService.postRequest(url, properties);
            contact.setId(jsonBody.getObject().getLong("vid"));
            return contact;
        } catch (HubSpotException e) {
            throw new HubSpotException("Cannot create contact: " + contact.getEmail() + ". Reason: " + e.getMessage(), e);
        }
    }

    public static HSContact update(HSContact contact) throws HubSpotException {
        if (contact.getId() == 0) {
            throw new HubSpotException("User ID must be provided");
        }

        String url = "/contacts/v1/contact/vid/" + contact.getId() + "/profile";
        String properties = contact.toJsonString();

        try {
            JsonNode jsonBody = HttpService.postRequest(url, properties);
            return contact;
        } catch (HubSpotException e) {
            throw new HubSpotException("Cannot update contact: " + contact.getId() + ". Reason: " + e.getMessage(), e);
        }
    }

    public static HSContact updateOrCreate(HSContact contact) throws HubSpotException {
        if (Strings.isNullOrEmpty(contact.getEmail())) {
            throw new HubSpotException("User email must be provided");
        }

        String url = "/contacts/v1/contact/createOrUpdate/email/" + contact.getEmail();
        String properties = contact.toJsonString();

        try {
            JsonNode jsonBody = HttpService.postRequest(url, properties);
            contact.setId(jsonBody.getObject().getLong("vid"));
            return contact;
        } catch (HubSpotException e) {
            throw new HubSpotException("Cannot update or create contact: " + contact.getEmail() + ". Reason: " + e.getMessage(), e);
        }
    }

    public static void delete(HSContact contact) throws HubSpotException {
        if (contact.getId() == 0) {
            throw new HubSpotException("User ID must be provided");
        }
        String url = "/contacts/v1/contact/vid/" + contact.getId();

        try {
            JsonNode jsonBody = HttpService.deleteRequest(url);
        } catch (HubSpotException e) {
            throw new HubSpotException("Cannot update contact: " + contact.getId() + ". Reason: " + e.getMessage(), e);
        }
    }

    private static HSContact parseContactData(JsonNode jsonBody) {
        HSContact contact = new HSContact();

        contact.setId(jsonBody.getObject().getLong("vid"));

        JSONObject jsonProperties = jsonBody.getObject().getJSONObject("properties");

        Set<String> keys = jsonProperties.keySet();

        keys.stream().forEach( key ->
                contact.setProperty(key,
                        jsonProperties.get(key) instanceof JSONObject ?
                                ((JSONObject) jsonProperties.get(key)).getString("value") :
                                jsonProperties.get(key).toString()
                )
        );

        return contact;
    }
}
