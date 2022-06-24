package de.androidcrypto.nfcnfcaauthprotection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class SpecialSettingsActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    Button enableCounter, disableCounter;
    Button enablueUidMirror, disableUidMirror;
    EditText task, commandReponse;
    private NfcAdapter mNfcAdapter;

    final String ENABLE_COUNTER_TASK = "enable counter";
    final String DISABLE_COUNTER_TASK = "disable counter";
    final String ENABLE_UID_MIRROR_TASK = "enable Uid mirror";
    final String DISABLE_UID_MIRROR_TASK = "disable Uid mirror";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_special_settings);

        enableCounter = findViewById(R.id.btnSpecialSettingEnableCounter);
        disableCounter = findViewById(R.id.btnSpecialSettingDisableCounter);
        enablueUidMirror = findViewById(R.id.btnSpecialSettingEnableUidMirror);
        disableUidMirror = findViewById(R.id.btnSpecialSettingDisableCounter);

        task = findViewById(R.id.etSpecialSettingsTask);
        commandReponse = findViewById(R.id.etSpecialSettingsResponse);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        enableCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(ENABLE_COUNTER_TASK);
            }
        });

        disableCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(DISABLE_COUNTER_TASK);
            }
        });

        enablueUidMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(ENABLE_UID_MIRROR_TASK);
            }
        });

        disableUidMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(DISABLE_UID_MIRROR_TASK);
            }
        });
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");

        NfcA nfcA = null;

        try {
            nfcA = NfcA.get(tag);

            if (nfcA != null) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "NFC tag is Nfca compatible",
                            Toast.LENGTH_SHORT).show();
                });

                // Make a Sound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);
                }

                runOnUiThread(() -> {
                    commandReponse.setText("");
                });

                nfcA.connect();

                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (!ntagVersion.equals("216")) {
                    writeToUiAppend(commandReponse, "NFC tag is NOT of type NXP NTAG216");
                    writeToUiToast("NFC tag is NOT of type NXP NTAG216");
                    return;
                }

                int nfcaMaxTranceiveLength = nfcA.getMaxTransceiveLength(); // important for the readFast command
                int ntagPages = NfcIdentifyNtag.getIdentifiedNtagPages();
                int ntagMemoryBytes = NfcIdentifyNtag.getIdentifiedNtagMemoryBytes();
                String tagIdString = getDec(tag.getId());
                String nfcaContent = "raw data of " + NfcIdentifyNtag.getIdentifiedNtagType() + "\n" +
                        "number of pages: " + ntagPages +
                        " total memory: " + ntagMemoryBytes +
                        " bytes\n" +
                        "tag ID: " + bytesToHex(NfcIdentifyNtag.getIdentifiedNtagId()) + "\n" +
                        "tag ID: " + tagIdString + "\n";
                nfcaContent = nfcaContent + "maxTranceiveLength: " + nfcaMaxTranceiveLength + " bytes\n";
                // read the complete memory depending on ntag type
                byte[] ntagMemory = new byte[ntagMemoryBytes];
                // read the content of the tag in several runs
                byte[] response = new byte[0];

                try {

                    // get command from task
                    String taskString = task.getText().toString();

                    switch (taskString) {
                        case ENABLE_COUNTER_TASK: {
                                response = writeEnableCounter(nfcA);
                                if (response == null) {
                                    writeToUiAppend(commandReponse, "Enabling the counter: FAILURE");
                                    return;
                                } else {
                                    writeToUiAppend(commandReponse, "Enabling the counter: SUCCESS - code: " + bytesToHex(response));
                                }
                            break;
                        }
                        case DISABLE_COUNTER_TASK: {
                            response = writeDisableCounter(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Disabling the counter: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Disabling the counter: SUCCESS - code: " + bytesToHex(response));
                            }
                            break;
                        }
                        case ENABLE_UID_MIRROR_TASK: {
                            response = writeEnableUidMirror(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Enabling the Uid mirror: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Enabling the Uid mirror: SUCCESS - code: " + bytesToHex(response));
                            }
                            break;
                        }
                        case DISABLE_UID_MIRROR_TASK: {
                            response = writeDisableUidMirror(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Enabling the Uid mirror: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Disabling the Uid mirror: SUCCESS - code: " + bytesToHex(response));
                            }
                            break;
                        }

                        default: {
                            // to task
                            writeToUiAppend(commandReponse, "choose a task by pressing the button");
                            return;
                        }
                    }

                } finally {
                    try {
                        nfcA.close();
                    } catch (IOException e) {
                        writeToUiAppend(commandReponse, "ERROR: IOException " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            writeToUiAppend(commandReponse, "ERROR: IOException " + e.toString());
            e.printStackTrace();
        }
    }

    // position is 0 based starting from right to left
    private byte setBitInByte(byte input, int pos) {
        return (byte) (input | (1 << pos));
    }

    // position is 0 based starting from right to left
    private byte unsetBitInByte(byte input, int pos) {
        return (byte) (input & ~(1 << pos));
    }


    private void writeToUi2(TextView textView, String message) {
        runOnUiThread(() -> {
            textView.setText(message);
        });
    }

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
            String newString = message + "\n" + textView.getText().toString();
            textView.setText(newString);
        });
    }

    private void writeToUiToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private byte[] writeEnableCounter(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the counter is in pages 42/132/228 (0x2A / 0x84 / 0xE4)
         * depending on the tag type
         *
         * byte 0 of this pages holds the ACCESS byte
         * bit 4 is the counter enabling flag, 0 = disabled, 1 = enabled
         *
         * bit 3 is the counter password protection 0 = NFC counter not protected, 1 = enabled
         * If the NFC counter password protection is enabled, the NFC tag will only respond to a
         * READ_CNT command with the NFC counter value after a valid password verification
         * This bit is NOT set in this command
         */

        writeToUiAppend(commandReponse, "* Start enabling the counter *");
        // first read the page, set bit to 1 and save the page back to the tag
        // read page 228 = Configuration page 1
        byte[] readPageResponse = getTagDataResponse(nfcA, 228); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = ACCESS byte
            byte accessByte = readPageResponse[0];
            writeToUiAppend(commandReponse, "ACCESS content old: " + printByteBinary(accessByte));
            // setting bit 4
            byte accessByteNew;
            accessByteNew = setBitInByte(accessByte, 4);
            writeToUiAppend(commandReponse, "ACCESS content new: " + printByteBinary(accessByteNew));
            // rebuild the page data
            readPageResponse[0] = accessByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 228, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    private byte[] writeDisableCounter(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the counter is in pages 42/132/228 (0x2A / 0x84 / 0xE4)
         * depending on the tag type
         *
         * byte 0 of this pages holds the ACCESS byte
         * bit 4 is the counter enabling flag, 0 = disabled, 1 = enabled
         *
         * bit 3 is the counter password protection 0 = NFC counter not protected, 1 = enabled
         * If the NFC counter password protection is enabled, the NFC tag will only respond to a
         * READ_CNT command with the NFC counter value after a valid password verification
         * This bit is NOT set in this command
         */

        writeToUiAppend(commandReponse, "* Start disabling the counter *");
        // first read the page, set bit to 0 and save the page back to the tag
        // read page 228 = Configuration page 1
        byte[] readPageResponse = getTagDataResponse(nfcA, 228); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = ACCESS byte
            byte accessByte = readPageResponse[0];
            writeToUiAppend(commandReponse, "ACCESS content old: " + printByteBinary(accessByte));
            // setting bit 4
            byte accessByteNew;
            accessByteNew = unsetBitInByte(accessByte, 4);
            writeToUiAppend(commandReponse, "ACCESS content new: " + printByteBinary(accessByteNew));
            // rebuild the page data
            readPageResponse[0] = accessByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 228, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    // todo change description (it is the old one forr counter)
    private byte[] writeEnableUidMirror(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the counter is in pages 42/132/228 (0x2A / 0x84 / 0xE4)
         * depending on the tag type
         *
         * byte 0 of this pages holds the ACCESS byte
         * bit 4 is the counter enabling flag, 0 = disabled, 1 = enabled
         *
         * bit 3 is the counter password protection 0 = NFC counter not protected, 1 = enabled
         * If the NFC counter password protection is enabled, the NFC tag will only respond to a
         * READ_CNT command with the NFC counter value after a valid password verification
         * This bit is NOT set in this command
         */

        writeToUiAppend(commandReponse, "* Start enabling the Uid mirror *");
        // first read the page, set bit to 1 and save the page back to the tag
        // read page 228 = Configuration page 1
        byte[] readPageResponse = getTagDataResponse(nfcA, 228); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = ACCESS byte
            byte accessByte = readPageResponse[0];
            writeToUiAppend(commandReponse, "ACCESS content old: " + printByteBinary(accessByte));
            // setting bit 4
            byte accessByteNew;
            accessByteNew = setBitInByte(accessByte, 4);
            writeToUiAppend(commandReponse, "ACCESS content new: " + printByteBinary(accessByteNew));
            // rebuild the page data
            readPageResponse[0] = accessByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 228, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }


    // todo change description (it is the old one forr counter)
    private byte[] writeDisableUidMirror(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the uid mirror is in pages 41/131/227 (0x29 / 0x83 / 0xE3)
         * depending on the tag type
         *
         * byte 0 of this pages holds the MIRROR byte
         * byte 2 of this pages holds the MIRROR_PAGE byte
         *
         * bit 4 is the counter enabling flag, 0 = disabled, 1 = enabled
         *
         * bit 3 is the counter password protection 0 = NFC counter not protected, 1 = enabled
         * If the NFC counter password protection is enabled, the NFC tag will only respond to a
         * READ_CNT command with the NFC counter value after a valid password verification
         * This bit is NOT set in this command
         */

        writeToUiAppend(commandReponse, "* Start disabling the Uid mirror *");
        // first read the page, set bit to 0 and save the page back to the tag
        // read page 228 = Configuration page 1
        byte[] readPageResponse = getTagDataResponse(nfcA, 228); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = ACCESS byte
            byte accessByte = readPageResponse[0];
            writeToUiAppend(commandReponse, "ACCESS content old: " + printByteBinary(accessByte));
            // setting bit 4
            byte accessByteNew;
            accessByteNew = unsetBitInByte(accessByte, 4);
            writeToUiAppend(commandReponse, "ACCESS content new: " + printByteBinary(accessByteNew));
            // rebuild the page data
            readPageResponse[0] = accessByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 228, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    private byte[] getTagDataResponse(NfcA nfcA, int page) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0x30,  // READ
                (byte) (page & 0x0ff), // page 0
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(commandReponse, "Error on reading page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(commandReponse, "Error (NACK) on reading page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(commandReponse, "SUCCESS on reading page " + page + " response: " + bytesToHex(response));
                System.out.println("reading page " + page + ": " + bytesToHex(response));
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(commandReponse, "ERROR: Tag lost exception on reading");
            return null;
        } catch (IOException e) {
            writeToUiAppend(commandReponse, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private byte[] writeTagDataResponse(NfcA nfcA, int page, byte[] dataByte) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0xA2,  // WRITE
                (byte) (page & 0x0ff),
                dataByte[0],
                dataByte[1],
                dataByte[2],
                dataByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(commandReponse, "Error on writing page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(commandReponse, "Error (NACK) on writing page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(commandReponse, "SUCCESS on writing page " + page + " response: " + bytesToHex(response));
                System.out.println("response page " + page + ": " + bytesToHex(response));
                return response;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(commandReponse, "ERROR: Tag lost exception");
            return null;
        } catch (IOException e) {
            writeToUiAppend(commandReponse, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private boolean writeTagData(NfcA nfcA, int page, byte[] dataByte, TextView textView,
                                 byte[] response) {
        boolean result;
        //byte[] response;
        byte[] command = new byte[]{
                (byte) 0xA2,  // WRITE
                (byte) (page & 0x0ff), // page
                dataByte[0],
                dataByte[1],
                dataByte[2],
                dataByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "ERROR: null response");
                return false;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "ERROR: NACK response: " + bytesToHex(response));
                return false;
            } else {
                // success: response contains (P)ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + bytesToHex(response));
                System.out.println("write to page " + page + ": " + bytesToHex(response));
                result = true;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception on writing");
            return false;
        } catch (IOException e) {
            writeToUiAppend(textView, "IOException: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return result; // response contains the response
    }

    private boolean getTagData(NfcA nfcA, int page, TextView textView, byte[] response) {
        boolean result;
        //byte[] response;
        byte[] command = new byte[]{
                (byte) 0x30,  // READ
                (byte) (page & 0x0ff),
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "ERROR: null response");
                return false;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "ERROR: NACK response: " + bytesToHex(response));
                return false;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + bytesToHex(response));
                System.out.println("read from page " + page + ": " + bytesToHex(response));
                result = true;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception");
            return false;
        } catch (IOException e) {
            writeToUiAppend(textView, "IOException: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes)
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static String removeAllNonAlphaNumeric(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    private String getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result + "";
    }

    private static String printByteBinary(byte bytes){
        byte[] data = new byte[1];
        data[0] = bytes;
        return printByteArrayBinary(data);
    }

    private static String printByteArrayBinary(byte[] bytes){
        String output = "";
        for (byte b1 : bytes){
            String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
            //s1 += " " + Integer.toHexString(b1);
            //s1 += " " + b1;
            output = output + " " + s1;
            //System.out.println(s1);
        }
        return output;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

}