package com.snaptechnology.bgonzalez.bookmyroomandroid.activity;

/**
 * Home Fragment, you can book and delete events from this fragment
 */

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import cn.pedant.SweetAlert.SweetAlertDialog;
import com.snaptechnology.bgonzalez.bookmyroomandroid.R;
import com.snaptechnology.bgonzalez.bookmyroomandroid.model.Attendee;
import com.snaptechnology.bgonzalez.bookmyroomandroid.model.Event;
import com.snaptechnology.bgonzalez.bookmyroomandroid.model.Location;
import com.snaptechnology.bgonzalez.bookmyroomandroid.services.EventService;
import com.snaptechnology.bgonzalez.bookmyroomandroid.services.TimeService;
import com.snaptechnology.bgonzalez.bookmyroomandroid.utils.EventHandlerUtilities;
import com.snaptechnology.bgonzalez.bookmyroomandroid.utils.UtilProperties;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.extra.Scale;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HomeFragment extends Fragment {
    /**Variable used to check the last click by the user*/

    private static long mLastClickTime;


    /**Services to get the functions to do the task by the user*/
    private EventService eventService = EventService.getInstance(getActivity());
    private TimeService timeService = new TimeService();

    /** Current event to show to user, if is null is because there is not event at the moment*/
    private Event currentEvent;

    /** Skip Policies to make request to the server easier */
    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

    /** Constructor */
    public HomeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        StrictMode.setThreadPolicy(policy); // set policies to this fragment

        new Thread(new Runnable() {// Thread to refresh the home fragment
            @Override
            public void run() {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                refreshFragment();
            }
        }).start();

        // Main view of this fragment
        final View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Declarations to main circle to book and delete events
        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        final ViewGroup transitionsContainer = (ViewGroup) rootView.findViewById(R.id.transitions_container);

        // Get the status actual to the home fragment
        Map<String, String > mainData = getDataHome();

        if( mainData.get("state").equalsIgnoreCase("B")){ // in case of being busy

            int meetingTime = (int)( timeService.calculateDifferenceDates(currentEvent.getStart(), currentEvent.getEnd()) *.1 ) ;
            int lapsedMeetingTime =  (int) ( timeService.calculateDifferenceDates(currentEvent.getStart(),timeService.getActualTimeInString())*.1 );

            progressBar.bringToFront();
            progressBar.setVisibility(View.VISIBLE);

            ObjectAnimator animation = ObjectAnimator.ofInt (progressBar, "progress", lapsedMeetingTime * 500 / meetingTime, 500); // see this max value coming back here, we animale towards that value
            animation.setDuration (5000000);
            animation.setInterpolator (new DecelerateInterpolator());
            animation.start ();

            ImageView unavailableImage = (ImageView) rootView.findViewById(R.id.image_unavailable);
            unavailableImage.setVisibility(View.VISIBLE);
            unavailableImage.bringToFront();

            final TextView cancelButton = (TextView) transitionsContainer.findViewById(R.id.circle_finished);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();                    deleteEvent(currentEvent);
                }
            });

            transitionsContainer.findViewById(R.id.image_unavailable).setOnClickListener(new VisibleToggleClickListener() {
                @Override
                protected void changeVisibility(boolean visible) {
                    TransitionManager.beginDelayedTransition(transitionsContainer, new Scale());
                    cancelButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
                }
            });


        }
        else{// in the case of being free

            ImageView availableImage = (ImageView) rootView.findViewById(R.id.image_available);
            availableImage.setVisibility(View.VISIBLE);
            availableImage.bringToFront();

            //Store ids of view to each minute available
            final List<Integer> rIds = new ArrayList<>();
            rIds.add(R.id.fifteen_minutes);
            rIds.add(R.id.thirty_minutes);
            rIds.add(R.id.fortyFive_minutes);
            rIds.add(R.id.sixty_minutes);


            final String dateInString = timeService.getActualTimeInString();
            final List<String> availableMinutes = getAvailableMinutesToBook(dateInString);

            final String tmpEndDate = timeService.roundDateToHigherInString(dateInString);

            for(int i = 0; i < availableMinutes.size(); i++){
                TextView minutesToBook = (TextView) rootView.findViewById(rIds.get(i));

                final String endDate = timeService.addMinutes(tmpEndDate, i + 1 );
                minutesToBook.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { // To press save button in dialog showed

                        //EventHandlerUtilities.preventDoubleClick();
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                            return;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        LayoutInflater inflater = getActivity().getLayoutInflater();

                        final View dialogView =  inflater.inflate(R.layout.dialog_book_room_from_home,null);
                        builder.setView(dialogView);

                        final AlertDialog dialog = builder.create();

                        Button buttonBookRoom = (Button) dialogView.findViewById(R.id.btn_book_room_from_home);
                        buttonBookRoom.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //EventHandlerUtilities.preventDoubleClick();
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            String startDate = timeService.roundDateToLessInString(dateInString);
                            String subject = (((TextInputLayout) dialogView.findViewById(R.id.subject_book_room_from_home)).getEditText().getText()).toString();
                            Event event = new Event("id", subject, new Location(UtilProperties.getLocationProperty(getActivity())),new ArrayList<Attendee>(), false,  startDate,  endDate);
                            createEvent(event);

                            dialog.cancel();
                        }
                    });
                    dialog.show();
                    }
                });
            }

            transitionsContainer.findViewById(R.id.image_available).setOnClickListener(new VisibleToggleClickListener() {
                @Override
                protected void changeVisibility(boolean visible) {
                TransitionManager.beginDelayedTransition(transitionsContainer, new Scale());
                    for(int i = 0 ; i < availableMinutes.size(); i++){
                        (rootView.findViewById(rIds.get(i))).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
                    }
                }
            });
        }

        TextView availableIn =(TextView) rootView.findViewById(R.id.available_in);
        availableIn.setText(mainData.get("availableIn"));

        TextView nextMeeting =(TextView) rootView.findViewById(R.id.next_meeting);
        nextMeeting.setText(mainData.get("nextMeeting"));

        final TableLayout timeLine = (TableLayout) rootView.findViewById(R.id.time_line);

        final TableRow rowTimeLine  = (TableRow) timeLine.getChildAt(1);
        String start =  timeService.getInitialTimeDay();
        for(int i = 0 ; i < rowTimeLine.getChildCount(); i++){
            final TextView v = (TextView) rowTimeLine.getChildAt(i);

            if(eventService.getEventMapper().containsKey(start)){
                v.setBackgroundColor(Color.RED);
            }
            start = timeService.addMinutes(start);
        }

        return rootView;
    }

    /**
     * Method to get the available minutes to book a meeting
     * @param date
     * @return String list that contains available minutes
     */
    public List<String> getAvailableMinutesToBook(String date){

        List<String> availableMinutes = new ArrayList<>();
        int minutes = timeService.getMinMin();

        String limitDateOfDay = timeService.addADay(timeService.resetHoursStringDate(date));

        Date actual = timeService.convertStringToDate(date);
        date = timeService.roundDateToHigherInString(date);
        Date nextDay = timeService.convertStringToDate(limitDateOfDay);

        while(!eventService.getEventMapper().containsKey(date) && actual.compareTo(nextDay) < 0 && availableMinutes.size() < 4) {

            availableMinutes.add(Integer.toString(minutes) + " min");
            minutes += timeService.getMinMin();
            date = timeService.addMinutes(date);
            actual = timeService.convertStringToDate(date);
        }

        return availableMinutes;
    }

    /**
     * Method to get the data to fill the home fragment
     *
     * @return Map with current state and information of next meeting and when is available
     */
    private Map<String,String> getDataHome(){
        Map<String,String> dataHome = new HashMap<>();

        String currentDateInString = timeService.getActualTimeInString();
        boolean startIsHigherToCurrent;
        boolean endIsHigherToCurrent;

        int sizeEvents = eventService.getEvent().size();
        Event tmpEvent ;
        for(int i = 0 ; i < sizeEvents; i++){
            tmpEvent = eventService.getEvent().get(i);
            startIsHigherToCurrent = timeService.isGreaterDate(tmpEvent.getStart(), currentDateInString);
            endIsHigherToCurrent = timeService.isGreaterDate(tmpEvent.getEnd(), currentDateInString);

            //**Start and end are higher than current*/
            if( startIsHigherToCurrent && endIsHigherToCurrent ){
                dataHome.put("state","F");
                dataHome.put("availableIn","Now");
                dataHome.put("nextMeeting",timeService.calculateDifferenceInString(currentDateInString, eventService.getEvent().get(i).getStart()));
                currentEvent = null;
                return dataHome;
            }
            //**Start is less than current and end is higher than current */
            else if( !startIsHigherToCurrent && endIsHigherToCurrent  ){
                currentEvent = tmpEvent;
                dataHome.put("state","B");

                //**if exist other event in the week*/
                if ( i + 1  < sizeEvents){

                    dataHome.put("nextMeeting", timeService.calculateDifferenceInString(currentDateInString, eventService.getEvent().get(i+1).getStart()));
                    while( i + 1 < sizeEvents){
                        //** Compare actual with next event*/
                        if ( ! eventService.getEvent().get(i).getEnd().equalsIgnoreCase(eventService.getEvent().get( i + 1 ).getStart())){
                            dataHome.put("availableIn",timeService.calculateDifferenceInString(currentDateInString,eventService.getEvent().get(i).getEnd()));
                            return dataHome;
                        }
                        i++;
                    }
                    dataHome.put("availableIn",timeService.calculateDifferenceInString(currentDateInString,eventService.getEvent().get(sizeEvents - 1).getEnd()));
                    return dataHome;


                }else{
                    dataHome.put("nextMeeting", "Next Week");
                    dataHome.put("availableIn",timeService.calculateDifferenceInString(currentDateInString,tmpEvent.getEnd()));
                    dataHome.put("state","B");
                    return dataHome;
                }
            }
            //**Start and end are less than current*/
            else if( !startIsHigherToCurrent && !endIsHigherToCurrent  ){
                /**Pass to next event*/
            }
        }
        currentEvent = null;
        dataHome.put("state","F");
        dataHome.put("availableIn","Now");
        dataHome.put("nextMeeting","Next Week");

        return dataHome;
    }

    /**
     * Method to create a event from the GUI
     * @param event
     */
    private void createEvent(final Event event){
        final SweetAlertDialog pDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.PROGRESS_TYPE).setTitleText("Creating...");
        pDialog.show();
        pDialog.setCancelable(false);

        new CountDownTimer(800 * 7, 800) {
            boolean isExecuted = false;
            boolean isCreated = false;
            public void onTick(long millisUntilFinished) {
                try{
                    pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.blue_btn_bg_color));
                }catch (IllegalStateException e){
                    Log.e("Error","Error showing progress bar because user tried to create two event at same time");
                }
                while (isExecuted == false){
                    isExecuted = true;
                    isCreated = eventService.createEvent(event);
                }
            }

            public void onFinish() {
                if(isCreated) {
                    pDialog.setTitleText("Success!")
                            .setConfirmText("OK")
                            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);

                }else{
                    pDialog.setTitleText("Error! The meeting was not created")
                            .setConfirmText("OK")
                            .changeAlertType(SweetAlertDialog.ERROR_TYPE);
                }
                refreshFragment();

            }
        }.start();

    }

    /**
     * Method to delete a event from the GUI
     * @param event
     */
    private void deleteEvent(final Event event){
        final SweetAlertDialog sweetAlertDialog =  new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE);
                sweetAlertDialog.setTitleText("Are you sure?")
                .setContentText("Won't be able to recover this meeting!")
                .setCancelText("Cancel")
                .setConfirmText("Delete")
                .showCancelButton(true)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        final SweetAlertDialog pDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.PROGRESS_TYPE).setTitleText("Deleting");
                        pDialog.show();
                        pDialog.setCancelable(false);

                        new CountDownTimer(800 * 7, 800) {
                            boolean isExecuted = false;
                            boolean isDeleted = false;
                            public void onTick(long millisUntilFinished) {
                                try{
                                    pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.blue_btn_bg_color));
                                }catch (IllegalStateException e){
                                    Log.e("Error","Error showing progress bar because user tried to create two event at same time");
                                }
                                while (isExecuted == false){
                                    isExecuted = true;
                                    isDeleted = eventService.deleteEvent(event);
                                }
                            }

                            public void onFinish() {
                                if(isDeleted) {
                                    pDialog.setTitleText("Deleted it!")
                                            .setConfirmText("OK")
                                            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                                    sweetAlertDialog.cancel();


                                }else{
                                    pDialog.setTitleText("Error!")
                                            .setConfirmText("OK")
                                            .changeAlertType(SweetAlertDialog.ERROR_TYPE);
                                    sweetAlertDialog.cancel();
                                }

                                refreshFragment();

                            }
                        }.start();

                    }
                })
                .show();
    }

    /**
     * Method to refresh the fragment
     */
    private void refreshFragment(){
        try{
            Fragment fragment = new HomeFragment();
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container_body, fragment);
            fragmentTransaction.commitAllowingStateLoss();
        }catch ( NullPointerException e){
            Log.i("Warning refresh","Update fragment not completed");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
