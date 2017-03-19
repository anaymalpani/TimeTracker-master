package com.alacritic.timetracker.export_import;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.alacritic.timetracker.R;
import com.alacritic.timetracker.domain.TimeRecord;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class XmlExport extends AbstractExport {
    private static final SimpleDateFormat COMMENT_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    /**
     * Constructor
     */
    public XmlExport(Context context) {
        super(context);
    }

    @Override
    protected int getResourceIdMessageExportInProgress() {
        return R.string.exporting_backup_progress_dialog;
    }

    @Override
    protected int getResourceIdMessageExportCompleted() {
        return R.string.export_backup_completed_to_file;
    }

    @Override
    protected File doExport() {
        File xmlFile = null;
        FileWriter writer = null;

        Date backupCreationDate = new Date();

        if (isExternalStorageWritable()) {
            try {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TimeTracker");
                directory.mkdirs();

                xmlFile = new File(directory, "Backup-" + COMMENT_DATE_FORMAT.format(backupCreationDate) + ".xml");
                xmlFile.setReadable(true, false);
                xmlFile.setWritable(true, false);

                Log.d(LOG_TAG, "XML file: " + xmlFile.getAbsolutePath());

                XmlSerializer xmlSerializer = Xml.newSerializer();
                writer = new FileWriter(xmlFile);

                xmlSerializer.setOutput(writer);

                xmlSerializer.startDocument("UTF-8", true);

                xmlSerializer.startTag(null, ExportImportXmlElements.BACKUP);
                addCreationTimestamp(xmlSerializer, backupCreationDate);
                addApplicationVersion(xmlSerializer);
                addTimeRecords(xmlSerializer);
                xmlSerializer.endTag(null, ExportImportXmlElements.BACKUP);

                xmlSerializer.endDocument();

                xmlSerializer.flush();

            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
            }
        } else {
            Toast.makeText(context, R.string.export_external_storage_not_available, Toast.LENGTH_LONG).show();
        }
        return xmlFile;
    }

    private void addCreationTimestamp(XmlSerializer xmlSerializer, Date backupCreationDate) throws IOException {
        xmlSerializer.comment(toComment(backupCreationDate.getTime()));
        xmlSerializer.startTag(null, ExportImportXmlElements.CREATED_ON);
        xmlSerializer.text(Long.toString(backupCreationDate.getTime()));
        xmlSerializer.endTag(null, ExportImportXmlElements.CREATED_ON);
    }

    private void addApplicationVersion(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, ExportImportXmlElements.APPLICATION_VERSION);

        String version;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "?";
        }
        xmlSerializer.text(version);
        xmlSerializer.endTag(null, ExportImportXmlElements.APPLICATION_VERSION);
    }

    private void addTimeRecords(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, ExportImportXmlElements.TIME_RECORDS);

        Iterator<TimeRecord> timeRecordIterator = TimeRecord.findAsIterator(TimeRecord.class, null);
        while (timeRecordIterator.hasNext()) {
            TimeRecord timeRecord = timeRecordIterator.next();
            if (!timeRecord.isCheckIn()) {
                exportSingleTimeRecord(timeRecord, xmlSerializer);
            }
        }

        xmlSerializer.endTag(null, ExportImportXmlElements.TIME_RECORDS);
    }

    private void exportSingleTimeRecord(TimeRecord timeRecord, XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, ExportImportXmlElements.TIME_RECORD);

        xmlSerializer.comment(toComment(timeRecord.getCheckIn().getTimeInMillis()));
        xmlSerializer.startTag(null, ExportImportXmlElements.CHECKIN);
        xmlSerializer.text(Long.toString(timeRecord.getCheckIn().getTimeInMillis()));
        xmlSerializer.endTag(null, ExportImportXmlElements.CHECKIN);

        xmlSerializer.comment(toComment(timeRecord.getCheckOut().getTimeInMillis()));
        xmlSerializer.startTag(null, ExportImportXmlElements.CHECKOUT);
        xmlSerializer.text(Long.toString(timeRecord.getCheckOut().getTimeInMillis()));
        xmlSerializer.endTag(null, ExportImportXmlElements.CHECKOUT);

        xmlSerializer.startTag(null, ExportImportXmlElements.BREAK_IN_MILLIS);
        if (timeRecord.getBreakInMilliseconds() != null) {
            xmlSerializer.text(Long.toString(timeRecord.getBreakInMilliseconds()));
        }
        xmlSerializer.endTag(null, ExportImportXmlElements.BREAK_IN_MILLIS);

        xmlSerializer.startTag(null, ExportImportXmlElements.NOTE);
        if (timeRecord.getNote() != null) {
            xmlSerializer.text(timeRecord.getNote());
        }
        xmlSerializer.endTag(null, ExportImportXmlElements.NOTE);

        xmlSerializer.endTag(null, ExportImportXmlElements.TIME_RECORD);
    }

    private String toComment(long timeInMillis) {
        return COMMENT_DATE_FORMAT.format(new Date(timeInMillis));
    }
}
