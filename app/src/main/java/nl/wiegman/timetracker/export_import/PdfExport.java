package nl.wiegman.timetracker.export_import;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.wiegman.timetracker.R;
import nl.wiegman.timetracker.domain.TimeRecord;
import nl.wiegman.timetracker.util.Formatting;
import nl.wiegman.timetracker.period.Period;
import nl.wiegman.timetracker.util.TimeAndDurationService;

public class PdfExport extends AbstractExport {

    private final Period period;

    private final SimpleDateFormat dayInWeekFormat = new SimpleDateFormat("EEE dd-MM");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * Constructor
     */
    public PdfExport(Context context, Period period) {
        super(context);
        this.period = period;
    }

    @Override
    protected int getResourceIdMessageExportInProgress() {
        return R.string.exporting_pdf_progress_dialog;
    }

    @Override
    protected int getResourceIdMessageExportCompleted() {
        return R.string.export_pdf_completed_to_file;
    }

    protected File doExport() {
        File pdfFile = null;
        Document document = null;
        FileOutputStream outputStream = null;

        if (isExternalStorageWritable()) {
            try {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TimeTracker");
                directory.mkdirs();

                pdfFile = new File(directory, period.getTitle() + ".pdf");
                pdfFile.setReadable(true, false);
                pdfFile.setWritable(true, false);

                Log.d(LOG_TAG, "PDF file: " + pdfFile.getAbsolutePath());

                outputStream = new FileOutputStream(pdfFile);

                document = getDocument(outputStream);

                document.add(getTitle());
                document.add(Chunk.NEWLINE);
                document.add(createTable());

                document.close();
                outputStream.close();

            } catch (DocumentException | IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            } finally {
                if (document != null) {
                    document.close();
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
            }
        } else {
            Toast.makeText(context, R.string.export_external_storage_not_available, Toast.LENGTH_LONG).show();
        }
        return pdfFile;
    }

    /** Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    @Override
    protected void openExportedFile(File pdfFile) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.fromFile(pdfFile);
        i.setDataAndType(data, "application/pdf");
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(i);
    }

    private Document getDocument(FileOutputStream outputStream) throws DocumentException {
        Document document;
        document = new Document(PageSize.A4.rotate());

        document.setMargins(55, 55, 80, 80);
        PdfWriter.getInstance(document, outputStream);
        document.open();
        return document;
    }

    private Element getTitle() {
        Font font = new Font();
        font.setColor(BaseColor.BLACK);
        font.setStyle(Font.BOLD);
        font.setSize(22);
        return new Phrase(period.getTitle(), font);
    }

    public Element createTable() throws DocumentException {
        int numColumns = 6;
        PdfPTable table = new PdfPTable(numColumns);
        table.setHeaderRows(1);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidthPercentage(100);

        float[] columnWidths = new float[] {
                10f, // Date
                10f, // From
                10f, // To
                10f, // Pause
                10f, // Duration
                20f  // Note
            };
        table.setWidths(columnWidths);

        addTableHeader(table);
        long total = addTableContent(table);
        addTableFooter(table, total);

        return table;
    }

    private long addTableContent(PdfPTable table) {
        long totalBillableDuration = 0;

        Calendar day = (Calendar) period.getFrom().clone();
        while (day.getTimeInMillis() < period.getTo().getTimeInMillis()) {
            Calendar startOfDay = TimeAndDurationService.getStartOfDay(day);
            Calendar endOfDay = TimeAndDurationService.getEndOfDay(day);

            List<TimeRecord> timeRecordsOnDay = TimeAndDurationService.getTimeRecordsBetween(startOfDay, endOfDay);

            if (timeRecordsOnDay == null || timeRecordsOnDay.isEmpty()) {
                addRowForDayWithoutCheckin(table, day);
            } else {
                for (TimeRecord timeRecord : timeRecordsOnDay) {
                    addRowForTimeRecord(table, timeRecord);
                    totalBillableDuration += timeRecord.getBillableDuration();
                }
            }
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return totalBillableDuration;
    }

    private void addRowForDayWithoutCheckin(PdfPTable table, Calendar day) {
        addCell(table, dayInWeekFormat.format(day.getTime()));
        addCell(table, "");
        addCell(table, "");
        addCell(table, "");
        addCell(table, Formatting.formatDuration(0));
        addCell(table, "");
    }

    private void addRowForTimeRecord(PdfPTable table, TimeRecord timeRecord) {
        addCell(table, dayInWeekFormat.format(timeRecord.getCheckIn().getTime()));
        addCell(table, timeFormat.format(timeRecord.getCheckIn().getTime()));
        addCell(table, timeFormat.format(timeRecord.getCheckOut().getTime()));
        addCell(table, TimeUnit.MILLISECONDS.toMinutes(timeRecord.getBreakInMilliseconds()) + " " + context.getString(R.string.export_table_minutes));
        addCell(table, Formatting.formatDuration(timeRecord.getBillableDuration()));

        PdfPCell cell = new PdfPCell(new Phrase(timeRecord.getNote()));
        cell.setPaddingLeft(5);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table) {
        table.addCell(getHeaderCell(context.getString(R.string.export_table_header_date)));
        table.addCell(getHeaderCell(context.getString(R.string.export_table_header_from)));
        table.addCell(getHeaderCell(context.getString(R.string.export_table_header_to)));
        table.addCell(getHeaderCell(context.getString(R.string.export_table_header_break)));
        table.addCell(getHeaderCell(context.getString(R.string.export_table_header_duration)));
        table.addCell(getHeaderCell(context.getString(R.string.export_table_header_note)));
    }

    private void addTableFooter(PdfPTable table, long total) {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(4);
        table.addCell(cell);

        Font font = new Font();
        font.setStyle(Font.BOLD);
        PdfPCell totalCell = new PdfPCell(new Phrase(Formatting.formatDuration(total), font));
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalCell);

        cell = new PdfPCell();
        table.addCell(cell);
    }

    private PdfPCell getHeaderCell(String text) {
        Font font = new Font();
        font.setColor(BaseColor.WHITE);
        font.setStyle(Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BaseColor.GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }
}

