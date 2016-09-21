package com.snaptechnology.bgonzalez.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snaptechnology.bgonzalez.httpclient.ApacheHttpClient;
import com.snaptechnology.bgonzalez.model.Attendee;
import com.snaptechnology.bgonzalez.model.EmailAddress;
import com.snaptechnology.bgonzalez.model.Event;
import com.snaptechnology.bgonzalez.model.Location;
import com.snaptechnology.bgonzalez.model.VO.EventVO;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Office·65Service is a service that is used by Event Controller,
 * this service communicate with ApacheHttpClient to make request to the API Office 365
 *
 * @author Brayan González
 * @since 04/08/2016.
 */
@Service
public class Office365Service {

    private ApacheHttpClient client;
    private URLService urlService;
    private String delta="";

    final static Logger logger = Logger.getLogger(Office365Service.class);

    public Office365Service() {
        setUrlService(new URLService());
        setClient(new ApacheHttpClient());
    }


    /**
     * Method to get events to send to EventController
     * @param eventVO with fields location, startDate and endDate
     * @return events
     */
    public List<Event> getEvents(EventVO eventVO){

        logger.info("Getting status code to get events from Office365 Service");

        List<Event> events = new ArrayList<Event>();

        client.setLocation(eventVO.getLocation());
        urlService.setLocation(eventVO.getLocation());

        client.getHttpRequest(urlService.getURLEvents(eventVO.getStart(),eventVO.getEnd()));

        JSONArray json = new JSONObject(client.getOutput()).getJSONArray("value");
        for(int i = 0; i < json.length(); i++){
            events.add(new Event(json.getJSONObject(i)));
        }
        return events;
    }

    /**
     * Method to get status code of event creation to send to EventController
     * @param event
     * @return Operation status code
     */
    public int createEvent(Event event){

        logger.info("Getting status code to create event from Office365 Service");

        ObjectMapper mapper = new ObjectMapper();
        client.setLocation(event.getLocation());
        urlService.setLocation(event.getLocation());

        int statusCode = 0;
        try {
            statusCode = client.postHttpRequest(urlService.getURLCreateEvent(), mapper.writeValueAsString(event)).getStatusCode();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Error trying to get status code to create event in Office365 Service");
        }
        return statusCode;
    }


    public void updateEvent (Event event){
        //client.patchHttpRequest(urlService.getURLUpdateEvent(event.getId()),);
    }

    public List<Event> synchronizedEvents(Location location, String startDate, String endDate, String delta){
        List<Event> events = new ArrayList<Event>();



        client.getHttpRequest(urlService.getURLSynchronizeEvents(startDate, endDate, delta));
        String dataDelta = new JSONObject(client.getOutput()).getString("@odata.deltaLink");

        setDelta(dataDelta);
        System.out.println(urlService.getURLSynchronizeEvents(startDate, endDate, delta));

        JSONArray json = new JSONObject(client.getOutput()).getJSONArray("value");
        System.out.println(client.getOutput());

        for(int i = 0; i < json.length(); i++){
            events.add(new Event(json.getJSONObject(i)));
        }

        return events;
    }

    public String getDelta(){
        return delta;
    }

    public void setDelta(String dataDelta){
        /* This is because when is done a getEvents(), the parameter is deltatoken
        and when is done a synchronizedEvents() the parameter is deltaToken
        with T in capitalized
         */
        String[] arrayDataDelta = dataDelta.split("oken=");
        String delta = arrayDataDelta[1];
        this.delta = delta;
    }

    public URLService getUrlService() {
        return urlService;
    }

    private void setClient(ApacheHttpClient client) {
        this.client = client;
    }
    private void setUrlService(URLService urlService){
        this.urlService  = urlService;
    }




    public static void main(String[] args) throws JsonProcessingException {
        Office365Service rc = new Office365Service();
        ObjectMapper mapper = new ObjectMapper();

        // Create

        EmailAddress emailAddress = new EmailAddress("Brayan González","bgonzalez@snaptechnology.net");
        Attendee attendee = new Attendee(emailAddress,"Required");
        Location location = new Location("Bella");
        List<Attendee> attendees = new ArrayList<Attendee>();
        attendees.add(attendee);

        Event event = new Event("id","Test from Server",location,false,"2016-09-13T16:30:00.0003579Z","2016-09-13T20:30:00.0003579Z");
        String JSON = mapper.writeValueAsString(event);

        String test = "{\"displayNameLocation\":\"bgonzalez@snaptechnology.net\",\"data\":"+JSON+"}";
        System.out.println(test);
        //rc.createEvent(test);



        //System.out.println(mapper.writeValueAsString(rc.synchronizedEvents("2016-09-13T10:30:00.0003579Z","2016-09-13T11:30:00.0003579Z")));


































        //Event e = new Event("1","subject",null,null,null,false,"","");
        //System.out.println(mapper.writeValueAsString(e));

        /*String jsonInString = null;
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                jsonInString = mapper.writeValueAsString(rc.synchronizedEvents("2016-08-16T11:29:00.0003579Z", "2016-08-20T21:30:00.0003579Z"));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            System.out.println(jsonInString);
        }


        */
    }

}
