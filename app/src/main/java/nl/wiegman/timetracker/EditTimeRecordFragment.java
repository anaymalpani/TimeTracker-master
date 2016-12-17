package nl.wiegman.timetracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;
import nl.wiegman.timetracker.domain.TimeRecord;
import nl.wiegman.timetracker.util.TimeAndDurationService;

import static android.view.View.OnClickListener;

/**
 * Shows the details of the given TimeRecord.
 * Each detail can be edited.
 */
public class EditTimeRecordFragment extends Fragment {
    private static final String INSTANCE_STATE_KEY_FROM = "FROM";
    private static final String INSTANCE_STATE_KEY_TO = "TO";
    private static final String INSTANCE_STATE_KEY_BREAK = "BREAK";
    private static final String INSTANCE_STATE_KEY_NOTE = "NOTE";
    private static final String INSTANCE_STATE_KEY_ISCHECKIN = "ISCHECKIN";

    private static final String ARG_PARAM_TIMERECORD_ID = "timeRecordId";

    private static final String UNSET = "-";

    // The id of the timerecord to edit, or null if creating a new one
    private Long timeRecordId;

    private View rootView;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd-MM-yyyy");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private boolean isCheckin;
    private Calendar from;
    private Calendar to;
    private Long breakInMillis;
    private String note;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment EditTimeRecord.
     */
    public static EditTimeRecordFragment newInstance(Long timeRecordId) {
        EditTimeRecordFragment fragment = new EditTimeRecordFragment();
        Bundle args = new Bundle();
        if (timeRecordId != null) {
            args.putLong(ARG_PARAM_TIMERECORD_ID, timeRecordId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public EditTimeRecordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_add).setVisible(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_PARAM_TIMERECORD_ID)) {
                timeRecordId = getArguments().getLong(ARG_PARAM_TIMERECORD_ID);
            } else {
                timeRecordId = null;
            }
        }
        if (savedInstanceState == null) {
            setFieldValuesFromTimeRecord();
        } else {
            setFieldValuesFromInstanceState(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_edit_time_record, container, false);
        ButterKnife.bind(this, rootView);

        setTitle();
        showHideDeleteButton();
        refreshHmiFromFieldValues();
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(INSTANCE_STATE_KEY_FROM, from.getTimeInMillis());

        if (to != null) {
            outState.putLong(INSTANCE_STATE_KEY_TO, to.getTimeInMillis());
        }
        if (breakInMillis != null) {
            outState.putLong(INSTANCE_STATE_KEY_BREAK, breakInMillis);
        }
        outState.putString(INSTANCE_STATE_KEY_NOTE, note);
        outState.putBoolean(INSTANCE_STATE_KEY_ISCHECKIN, isCheckin);
    }

    @OnClick(R.id.saveTimeRecordButton)
    public void saveTimeRecordButtonOnClick(View view) {
        if (validationOk()) {
            TimeRecord timeRecord;
            if (EditTimeRecordFragment.this.timeRecordId != null) {
                timeRecord = TimeRecord.findById(TimeRecord.class, EditTimeRecordFragment.this.timeRecordId);
            } else {
                timeRecord = new TimeRecord();
            }
            timeRecord.setCheckIn(from);

            if (timeRecordId == null || !timeRecord.isCheckIn()) {
                timeRecord.setCheckOut(to);
            }

            timeRecord.setBreakInMilliseconds(breakInMillis);
            timeRecord.setNote(note);
            timeRecord.save();

            closeFragment();
        }
    }

    @OnClick(R.id.cancelButton)
    public void onClick(View view) {
        closeFragment();
    }

    private void showHideDeleteButton() {
        Button deleteButton = (Button) rootView.findViewById(R.id.deleteButton);
        if (timeRecordId == null) {
            LinearLayout buttonHolder = (LinearLayout) rootView.findViewById(R.id.timeRecordDetailsButtonHolder);
            buttonHolder.removeView(deleteButton);
        } else {
            deleteButton.setOnClickListener(new DeleteButtonOnClickListener());
        }
    }

    private void setTitle() {
        TextView title = (TextView) rootView.findViewById(R.id.title);
        if (timeRecordId == null) {
            title.setText(R.string.add_timerecord_title);
        } else {
            title.setText(R.string.edit_timerecord_title);
        }
    }

    private void setFieldValuesFromTimeRecord() {
        if (timeRecordId != null) {
            TimeRecord timeRecord = TimeRecord.findById(TimeRecord.class, timeRecordId);
            from = (Calendar) timeRecord.getCheckIn().clone();
            if (timeRecord.getCheckOut() != null) {
                to = (Calendar) timeRecord.getCheckOut().clone();
            }
            breakInMillis = timeRecord.getBreakInMilliseconds();
            note = timeRecord.getNote();
            isCheckin = timeRecord.isCheckIn();
        } else {
            from = Calendar.getInstance();
            to = Calendar.getInstance();
            breakInMillis = 0L;
            note = "";
            isCheckin = false;
        }
    }

    private void setFieldValuesFromInstanceState(Bundle savedInstanceState) {
        from = Calendar.getInstance();
        from.setTimeInMillis(savedInstanceState.getLong(INSTANCE_STATE_KEY_FROM));

        Long savedTo = (Long)savedInstanceState.get(INSTANCE_STATE_KEY_TO);
        if (savedTo != null) {
            to = Calendar.getInstance();
            to.setTimeInMillis(savedTo);
        }

        breakInMillis = (Long) savedInstanceState.get(INSTANCE_STATE_KEY_BREAK);
        note = savedInstanceState.getString(INSTANCE_STATE_KEY_NOTE);
        isCheckin = savedInstanceState.getBoolean(INSTANCE_STATE_KEY_ISCHECKIN);
    }

    private void refreshHmiFromFieldValues() {
        refreshFromDate();
        refreshFromTime();

        refreshToDate();
        refreshToTime();

        refreshBreak();
        refreshNote();
    }

    private void refreshNote() {
        TextView noteTextView = (TextView) rootView.findViewById(R.id.noteValueTextView);
        if (note == null || "".equals(note)) {
            noteTextView.setText(UNSET);
        } else {
            noteTextView.setText(note);
        }
        noteTextView.setOnClickListener(new NoteTextViewOnClickListener());
    }

    private void refreshFromDate() {
        TextView fromDateTextView = (TextView) rootView.findViewById(R.id.fromDateValueTextView);
        String formattedFromDate = dateFormat.format(from.getTime());
        fromDateTextView.setText(formattedFromDate);
        EditDateClickListener fromDateClickListener = new EditDateClickListener(from);
        fromDateTextView.setOnClickListener(fromDateClickListener);
    }

    private void refreshFromTime() {
        TextView fromTimeTextView = (TextView) rootView.findViewById(R.id.fromTimeValueTextView);
        String formattedFromTime = timeFormat.format(from.getTime());
        fromTimeTextView.setText(formattedFromTime);
        EditTimeClickListener fromTimeClickListener = new EditTimeClickListener(from);
        fromTimeTextView.setOnClickListener(fromTimeClickListener);
    }

    private void refreshToDate() {
        TextView toDateTextView = (TextView) rootView.findViewById(R.id.toDateValueTextView);
        if (isCheckin) {
            toDateTextView.setText(UNSET);
        } else {
            String formattedToDate = dateFormat.format(to.getTime());
            toDateTextView.setText(formattedToDate);
            EditDateClickListener toDateClickListener = new EditDateClickListener(to);
            toDateTextView.setOnClickListener(toDateClickListener);
        }
    }

    private void refreshToTime() {
        TextView toTimeTextView = (TextView) rootView.findViewById(R.id.toTimeValueTextView);
        if (isCheckin) {
            toTimeTextView.setText("");
        } else {
            String formattedToTime = timeFormat.format(to.getTime());
            toTimeTextView.setText(formattedToTime);
            EditTimeClickListener toTimeClickListener = new EditTimeClickListener(to);
            toTimeTextView.setOnClickListener(toTimeClickListener);
        }
    }

    private void refreshBreak() {
        if (isCheckin && breakInMillis == null) {
            Long defaultBreakDuration = TimeAndDurationService.getCheckIn().getDefaultBreakDuration();
            if (defaultBreakDuration > 0) {
                setBreak(defaultBreakDuration);
            } else {
                setBreak(null);
            }
        } else {
            setBreak(breakInMillis);
        }
    }

    private void setBreak(Long breakMs) {
        TextView breakTextView = (TextView) rootView.findViewById(R.id.breakValueTextView);

        Long breakInMinutes;
        String textToSet;

        if (breakMs == null) {
            textToSet = UNSET;
            breakInMinutes = 0L;
        } else {
            breakInMinutes = TimeUnit.MILLISECONDS.toMinutes(breakMs);
            textToSet = getString(R.string.break_minutes, breakInMinutes);
        }

        breakTextView.setText(textToSet);
        breakTextView.setOnClickListener(new BreakTextViewOnClickListener(breakInMinutes));
    }

    private class NoteTextViewOnClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            RelativeLayout linearLayout = new RelativeLayout(getActivity());
            EditText editText = new EditText(getActivity());
            editText.setText(note);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            linearLayout.setLayoutParams(params);
            linearLayout.addView(editText, layoutParams);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.note);
            alertDialogBuilder.setView(linearLayout);

            alertDialogBuilder
                    .setPositiveButton(getOkButtonTextResource(), new NoteSetListener(editText))
                    .setNegativeButton(getCancelButtonTextResource(), new DismissOnClickListener());
            final AlertDialog alertDialog = alertDialogBuilder.create();

            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
            alertDialog.show();

        }
    }

    private int getCancelButtonTextResource() {
        Resources res = Resources.getSystem();
        int idOfNegativeButtonResource = res.getIdentifier("cancel", "string", "android");
        if (idOfNegativeButtonResource == 0) {
            idOfNegativeButtonResource = android.R.string.cancel;
        }
        return idOfNegativeButtonResource;
    }

    private int getOkButtonTextResource() {
        Resources res = Resources.getSystem();
        int idOfPositiveButtonResource = res.getIdentifier("date_time_set", "string", "android");
        if (idOfPositiveButtonResource == 0) {
            idOfPositiveButtonResource = android.R.string.ok;
        }
        return idOfPositiveButtonResource;
    }

    /**
     * Opens a dialog in which a time can be selected.
     * The calendar is updated with the selected time when the user confirms.
     */
    private class EditTimeClickListener implements OnClickListener {
        private final Calendar calendarToEdit;

        public EditTimeClickListener(Calendar calendarToEdit) {
            this.calendarToEdit = calendarToEdit;
        }

        @Override
        public void onClick(View view) {
            TimePickerWithSecondsDialog.OnTimeSetListener timeSetListener = new TimeSetListener(calendarToEdit);
            TimePickerWithSecondsDialog timePickerDialog = new TimePickerWithSecondsDialog(getActivity(), timeSetListener, calendarToEdit.get(Calendar.HOUR_OF_DAY), calendarToEdit.get(Calendar.MINUTE), calendarToEdit.get(Calendar.SECOND), true);
            timePickerDialog.show();
        }
    }

    private class TimeSetListener implements TimePickerWithSecondsDialog.OnTimeSetListener {
        private final Calendar calendarToEdit;

        public TimeSetListener(Calendar calendarToEdit) {
            this.calendarToEdit = calendarToEdit;
        }
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute, int seconds) {
            calendarToEdit.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendarToEdit.set(Calendar.MINUTE, minute);
            calendarToEdit.set(Calendar.SECOND, seconds);

            refreshHmiFromFieldValues();
        }
    }

    /**
     * Opens a dialog in which a date can be selected.
     * The calendar is updated with the selected date when the user confirms.
     */
    private class EditDateClickListener implements OnClickListener {
        private final Calendar calendarToEdit;

        public EditDateClickListener(Calendar calendarToEdit) {
            this.calendarToEdit = calendarToEdit;
        }

        @Override
        public void onClick(View view) {
            DateSetListener dateSetListener = new DateSetListener(calendarToEdit);
            DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), dateSetListener, calendarToEdit.get(Calendar.YEAR), calendarToEdit.get(Calendar.MONTH), calendarToEdit.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        }
    }

    private class DateSetListener implements DatePickerDialog.OnDateSetListener {
        private final Calendar calendarToEdit;

        public DateSetListener(Calendar calendarToEdit) {
            this.calendarToEdit = calendarToEdit;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            calendarToEdit.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            calendarToEdit.set(Calendar.MONTH, monthOfYear);
            calendarToEdit.set(Calendar.YEAR, year);

            refreshHmiFromFieldValues();
        }
    }

    private void editBreak(long currentBreakInMinutes) {
        RelativeLayout linearLayout = new RelativeLayout(getActivity());
        final EditText editText = new EditText(getActivity());
        editText.setText(Long.toString(currentBreakInMinutes));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(3);
        editText.setFilters(filterArray);
        editText.selectAll();

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        linearLayout.setLayoutParams(params);
        linearLayout.addView(editText, layoutParams);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.break_in_minutes);
        alertDialogBuilder.setView(linearLayout);

        alertDialogBuilder
                .setPositiveButton(getOkButtonTextResource(), new BreakSelectionListener(editText))
                .setNegativeButton(getCancelButtonTextResource(), new DismissOnClickListener());
        final AlertDialog alertDialog = alertDialogBuilder.create();

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        alertDialog.show();
    }

    private void closeFragment() {
        getFragmentManager().popBackStack();
    }

    private class BreakSelectionListener implements DialogInterface.OnClickListener {
        private final EditText editText;

        public BreakSelectionListener(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void onClick(DialogInterface dialog, int selectedIndex) {
            String breakInMinutesInput = editText.getText().toString();
            long breakInMinutes = 0;
            if (!breakInMinutesInput.isEmpty()) {
                breakInMinutes = Long.parseLong(breakInMinutesInput);
            }
            breakInMillis = TimeUnit.MINUTES.toMillis(breakInMinutes);

            dialog.dismiss();

            refreshHmiFromFieldValues();
        }
    }

    private boolean validationOk() {
        boolean result = true;

        long fromTimeInMillis = from.getTimeInMillis();
        Long toTimeInMillis = to != null ? to.getTimeInMillis() : null;

        long breaky = 0;
        if (breakInMillis != null) {
            breaky = breakInMillis;
        }

        if (toTimeInMillis != null && fromTimeInMillis >= toTimeInMillis) {
            result = false;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.to_date_must_be_later_than_from_date)
                    .setNeutralButton(android.R.string.ok, new DismissOnClickListener())
                    .show();
        } else if (toTimeInMillis != null && ((toTimeInMillis - fromTimeInMillis) - breaky) <= 0) {
            result = false;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.break_bigger_than_billable_duration)
                    .setNeutralButton(android.R.string.ok, new DismissOnClickListener())
                    .show();
        }
        return result;
    }

    private class BreakTextViewOnClickListener implements OnClickListener {
        private final long breakInMinutes;

        public BreakTextViewOnClickListener(long breakInMinutes) {
            this.breakInMinutes = breakInMinutes;
        }

        @Override
        public void onClick(View view) {
            editBreak(breakInMinutes);
        }
    }

    private class DeleteButtonOnClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            DialogInterface.OnClickListener dialogClickListener = new DeleteConfirmationDialogOnClickListener();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.confirm_delete)
                    .setTitle(R.string.confirm)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, new DismissOnClickListener())
                    .show();
        }
    }

    private class DeleteConfirmationDialogOnClickListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    TimeRecord timeRecord = TimeRecord.findById(TimeRecord.class, timeRecordId);
                    TimeRecordDelete.run(timeRecord, getActivity());
                    dialog.dismiss();
                    closeFragment();
                    break;
                default:
                    dialog.dismiss();
                    break;
            }
        }
    }

    private class NoteSetListener implements DialogInterface.OnClickListener {
        private final EditText editText;

        public NoteSetListener(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            note = editText.getText().toString();
            dialog.dismiss();
            refreshHmiFromFieldValues();
        }
    }
}
