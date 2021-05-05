package com.example.demo.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vonage.client.VonageClient;
import com.vonage.client.sms.MessageStatus;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.messages.TextMessage;
import com.vonage.client.voice.ncco.Ncco;
import com.vonage.client.voice.ncco.TalkAction;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

@Component
public class Scheduler {

    //update zip  code to the your area
    public static final String PIN_CODE = "451001";

    public static final String WEB_URL =
            "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByPin?pincode="+PIN_CODE+"&date=";


    public final String PHONE_NUMBER = "91*******";

    @Scheduled(cron = "0 */1 * * * ?")
    public void cronJobSch() throws ParseException, JsonProcessingException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        System.out.println("Java cron job expression:: " + strDate);

        SimpleDateFormat formatter= new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm:ss z");

        for (int i=0;i<8;i++){
            Date date = new Date(System.currentTimeMillis());
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(Calendar.DATE, i);
            date = c.getTime();

            ResponseEntity<String> response = checkAndSendMessage(formatter.format(date));

        }



    }

    private ResponseEntity<String> checkAndSendMessage(String date) throws JsonProcessingException {
        System.out.println("_______________________________________________________________________________");
        System.out.println("starting for date "+ date);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response
                = restTemplate.getForEntity(WEB_URL+date, String.class);


        ObjectMapper mapper = new ObjectMapper();

        HashMap<Object, Object> map = mapper.readValue(response.getBody(), HashMap.class);


        ArrayList<HashMap<String, Object>> centers = (ArrayList<HashMap<String, Object>>) map.get("centers");

        for (HashMap<String, Object> key : centers
        ) {

            System.out.println(key.get("center_id"));

            String centerName = (String) key.get("name");


            ArrayList<HashMap<String, Integer>> sessions = (ArrayList<HashMap<String, Integer>>) key.get("sessions");

            for (HashMap<String, Integer> mp : sessions
            ) {

                if (mp.get("min_age_limit") == 18 && mp.get("available_capacity") > 0 ) {

                    sendSms(PHONE_NUMBER, centerName);

                }
            }
        }
        return response;
    }

    public void sendSms(String number, String center) {
        VonageClient client = VonageClient.builder().apiKey("ADD_YOUR_KEY_HERE").apiSecret("ADD_SEECRET_HERE").build();



        TextMessage message = new TextMessage("Vonage APIs",
                number,
                "vaccine is available please check at "+center
        );

        SmsSubmissionResponse response = client.getSmsClient().submitMessage(message);

        Ncco ncco = new Ncco(TalkAction.builder(                "vaccine is available please check at "+center
        ).build());

       // client.getVoiceClient().createCall(new Call("919425980946", "917738707816", ncco));



        if (response.getMessages().get(0).getStatus() == MessageStatus.OK) {
            System.out.println("Message sent successfully.");
        } else {
            System.out.println("Message failed with error: " + response.getMessages().get(0).getErrorText());
        }

    }

}
