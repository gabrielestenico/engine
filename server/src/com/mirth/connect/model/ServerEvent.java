/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.model;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.mirth.connect.donkey.model.event.Event;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("event")
public class ServerEvent extends Event implements Serializable {
    public static final String ATTR_EXCEPTION = "Exception";

    public enum Level {
        INFORMATION, WARNING, ERROR
    }

    public enum Outcome {
        SUCCESS, FAILURE
    }

    private int id;
    private Calendar eventTime;
    private Level level = Level.INFORMATION;
    private String name;
    private Map<String, String> attributes = new LinkedHashMap<String, String>();
    private Outcome outcome = Outcome.SUCCESS;
    private int userId = 0;
    private String patientId;
    private String ipAddress;
    private String serverId;
    private String channelId;
    private String channelName;
    private String messageId;


	public ServerEvent() {
        eventTime = Calendar.getInstance();
        eventTime.setTimeInMillis(getDateTime());
    }

    public ServerEvent(String serverId, String name) {
        this();
        this.serverId = serverId;
        this.name = name;
    }

    public ServerEvent(String serverId, String name, Level level, Outcome outcome, Map<String, String> attributes) {
        this();
        this.serverId = serverId;
        this.name = name;
        this.level = level;
        this.outcome = outcome;
        this.attributes = attributes;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Calendar getEventTime() {
        return eventTime;
    }

    public void setEventTime(Calendar eventTime) {
        this.eventTime = eventTime;
    }

    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String name, String value) {
        if (attributes == null) {
            attributes = new LinkedHashMap<String, String>();
        }

        attributes.put(name, value);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    private String cleanString(String value) {
        if (value==null) return value;
        return value.trim().replaceAll("[\n\r]$", "");
    }
    
    public String getPatientId() {
    	this.patientId = cleanString(this.attributes.get("patientId"));
		return patientId;
	}

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public String getMessageId() {
        try {
            this.messageId = cleanString(this.attributes.get("messageId"));;
            return messageId;
        } catch (Exception e) {
            return null;
        }
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
   
    public String getChannelId() {
        try {
          String channel = cleanString(this.attributes.get("channel"));
          String channelId = (channel!=null && channel.contains("id")?channel.substring(channel.indexOf("id=") + 3, channel.indexOf(",")):null);
          this.channelId = channelId;
          return channelId;
        } catch (Exception e) {
          return null;
        }
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    } 

    public String getChannelName() {
        try {
          String channel = this.attributes.get("channel");
          
          String channelName = (channel!=null && channel.contains("name")?channel.substring(channel.indexOf("name=") + 5, channel.indexOf("]")):null);
          this.channelName = channelName;
          return channelName;
        } catch (Exception e) {
          return null;
        }
    }
    
    public String getChannelIdWithMessageId() {
        String channelId = getChannelId();
        String messageId = getMessageId();
        return (channelId !=null ? channelId + (messageId != null ? " - " + messageId : "") : null);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, CalendarToStringStyle.instance());
    }

    public static String getExportHeader() {
        return "ID, Date and Time, Level, Outcome, Name, User ID, IP Address, Attributes, ChannelID-MessageID, ChannelName, PatientID";
    }

    public String toExportString() {
        StringBuilder builder = new StringBuilder();
        builder.append(id + ", ");
        builder.append(new SimpleDateFormat(Exportable.DATE_TIME_FORMAT).format(eventTime.getTime()) + ", ");
        builder.append(level + ", ");
        builder.append(outcome + ", ");
        builder.append(name + ", ");
        builder.append(userId + ", ");
        builder.append(ipAddress + ", ");      

        /*
         * Print out the attributes and Base64 encode them in case there are newlines.
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        MapUtils.verbosePrint(ps, "attributes", attributes);
        builder.append(Base64.encodeBase64URLSafeString(baos.toByteArray()));
        IOUtils.closeQuietly(ps);
        
        builder.append("," + getChannelIdWithMessageId() + ",");
        builder.append(getChannelName() + ",");
        builder.append(getPatientId());
        builder.append(System.getProperty("line.separator"));

        return builder.toString();
    }
    
    public String toExportStringWithNoId() {
        StringBuilder builder = new StringBuilder();
        
        // Round the event time because SQLServer automatically rounds the milliseconds of its
        // DATETIMEs. This allows us to consistently compare ServerEvents from the database
        // to in-memory ones.
        Date roundedEventTime = new Date(Math.round(eventTime.getTimeInMillis() / 1000.0));
        builder.append(new SimpleDateFormat(Exportable.DATE_TIME_FORMAT).format(roundedEventTime) + ", ");
        
        builder.append(level + ", ");
        builder.append(outcome + ", ");
        builder.append(name + ", ");
        builder.append(userId + ", ");
        builder.append(ipAddress + ", ");      

        /*
         * Print out the attributes and Base64 encode them in case there are newlines.
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        MapUtils.verbosePrint(ps, "attributes", attributes);
        builder.append(Base64.encodeBase64URLSafeString(baos.toByteArray()));
        IOUtils.closeQuietly(ps);
        
        builder.append("," + getChannelIdWithMessageId() + ",");
        builder.append(getChannelName() + ",");
        builder.append(getPatientId());
        builder.append(System.getProperty("line.separator"));

        return builder.toString();
    }
}
