package de.androidcrypto.nfcnfcaauthprotection;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WriteActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    com.google.android.material.textfield.TextInputLayout passwordDecoration, packDecoration, inputFieldDecoration;
    com.google.android.material.textfield.TextInputEditText passwordField, packField, inputField;
    com.google.android.material.switchmaterial.SwitchMaterial authenticationSwitch;
    TextView nfcResult;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        inputField = findViewById(R.id.etWriteAuthInputField);
        inputFieldDecoration = findViewById(R.id.etWriteAuthInputFieldDecoration);
        nfcResult = findViewById(R.id.tvWriteAuthNfcaResult);
        authenticationSwitch = findViewById(R.id.swWriteAuth);
        passwordField = findViewById(R.id.etWriteAuthPassword);
        passwordDecoration = findViewById(R.id.etWriteAuthPasswordDecoration);
        packField = findViewById(R.id.etWriteAuthPack);
        packDecoration = findViewById(R.id.etWriteAuthPackDecoration);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        authenticationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    passwordDecoration.setVisibility(View.VISIBLE);
                    packDecoration.setVisibility(View.VISIBLE);
                } else {
                    passwordDecoration.setVisibility(View.GONE);
                    packDecoration.setVisibility(View.GONE);
                }
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
                writeToUiToast("NFC tag is Nfca compatible");

                // Make a Sound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);
                }

                nfcA.connect();

                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (ntagVersion.equals("0")) {
                    writeToUiAppend(nfcResult, "NFC tag is NOT of type NXP NTAG213/215/216");
                    writeToUiToast("NFC tag is NOT of type NXP NTAG213/215/216");
                    return;
                }

                int nfcaMaxTranceiveLength = nfcA.getMaxTransceiveLength(); // important for the readFast command
                int ntagPages = NfcIdentifyNtag.getIdentifiedNtagPages();
                int ntagMemoryBytes = NfcIdentifyNtag.getIdentifiedNtagMemoryBytes();
                String tagIdString = Utils.getDec(tag.getId());
                String nfcaContent = "raw data of " + NfcIdentifyNtag.getIdentifiedNtagType() + "\n" +
                        "number of pages: " + ntagPages +
                        " total memory: " + ntagMemoryBytes +
                        " bytes\n" +
                        "tag ID: " + Utils.bytesToHex(NfcIdentifyNtag.getIdentifiedNtagId()) + "\n" +
                        "tag ID: " + tagIdString + "\n";
                nfcaContent = nfcaContent + "maxTranceiveLength: " + nfcaMaxTranceiveLength + " bytes\n";
                // read the complete memory depending on ntag type
                byte[] ntagMemory = new byte[ntagMemoryBytes];
                // read the content of the tag in several runs
                byte[] response = new byte[0];
                try {
                    // do we want to write with or without authentication ?
                    boolean writeWithAuthentication = authenticationSwitch.isChecked();
                    if (writeWithAuthentication) {
                        // get data from passwordField
                        String passwordString = passwordField.getText().toString();
                        // limitation: exact 4 alphanumerical characters
                        passwordString = Utils.removeAllNonAlphaNumeric(passwordString);
                        if (passwordString.length() != 4) {
                            nfcaContent = nfcaContent + "Error: you need to enter exact 4 alphanumerical characters for PASSWORD" + "\n";
                            writeToUiAppend(nfcResult, nfcaContent);
                            return;
                        }
                        byte[] passwordByte = passwordString.getBytes(StandardCharsets.UTF_8);
                        int passwordLength = passwordByte.length;
                        nfcaContent = nfcaContent + "Password: " + passwordString + " hex: " + Utils.bytesToHex(passwordByte) + "\n";

                        // get pack from etWriteProtectionPack
                        String packString = packField.getText().toString();
                        // limitation: exact 2 alphanumerical characters
                        packString = Utils.removeAllNonAlphaNumeric(packString);
                        if (packString.length() != 2) {
                            nfcaContent = nfcaContent + "Error: you need to enter exact 2 alphanumerical characters for PACK" + "\n";
                            writeToUiAppend(nfcResult, nfcaContent);
                            return;
                        }
                        byte[] packByte = packString.getBytes(StandardCharsets.UTF_8);
                        int packLength = packByte.length;
                        nfcaContent = nfcaContent + "Pack: " + packString + " hex: " + Utils.bytesToHex(packByte) + "\n";
                        // as we write a complete page we need to fill up the bytes 3 + 4 with 0x00
                        byte[] packBytePage = new byte[4];
                        System.arraycopy(packByte, 0, packBytePage, 0, 2);

                        // send the pwdAuth command
                        // this is the default value
                        byte[] passwordByteDefault = new byte[]{
                                (byte) (255 & 0x0ff),
                                (byte) (255 & 0x0ff),
                                (byte) (255 & 0x0ff),
                                (byte) (255 & 0x0ff)
                        };
                        //passwordByte = passwordByteDefault.clone();
                        writeToUiAppend(nfcResult, "*** start authentication");
                        response = sendPwdAuthData(nfcA, passwordByte, nfcResult);
                        if (response == null) {
                            writeToUiAppend(nfcResult, "authentication FAILURE. Maybe wrong password or the tag is not write protected");
                            return;
                        }
                        byte[] packResponse = response.clone();
                        if (Arrays.equals(packResponse, packByte)) {
                            writeToUiAppend(nfcResult, "The entered PACK is correct");
                        } else {
                            writeToUiAppend(nfcResult, "entered PACK: " + Utils.bytesToHex(packByte));
                            writeToUiAppend(nfcResult, "Respons PACK: " + Utils.bytesToHex(packResponse));
                            writeToUiAppend(nfcResult, "The entered PACK is NOT correct, abort");
                            writeToUiToast("The entered PACK is NOT correct, abort");
                            return;
                        }

                    } // writeWithAuthentication

                    //
                    // get data from InputField
                    String dataString = inputField.getText().toString();
/*
                    // limitation: maximal 8 characters
                    if (dataString.length() > 8 ) {
                        dataString = dataString.substring(0, 8);
                    }
*/
                    byte[] dataByte = dataString.getBytes(StandardCharsets.UTF_8);
                    int dataLength = dataByte.length;
                    // as the Tag is saving in blocks of 4 bytes we need to know how many pages we do need
                    int dataPages = dataLength / 4;
                    int dataPagesMod = dataLength % 4; // if there is a remainder we need to use a new page to write
                    nfcaContent = nfcaContent + "data length: " + dataLength + "\n";
                    nfcaContent = nfcaContent + "data: " + Utils.bytesToHex(dataByte) + "\n";
                    nfcaContent = nfcaContent + "dataPages: " + dataPages + "\n";
                    nfcaContent = nfcaContent + "dataPagesMod: " + dataPagesMod + "\n";

                    // check that the data is fitting on the tag
                    if (dataLength > ntagMemoryBytes) {
                        writeToUiAppend(nfcResult, "data in InputField is too long for tag");
                        writeToUiToast("data in InputField is too long for tag");
                        return;
                    }
                    nfcaContent = nfcaContent + "writing full pages" + "\n";
                    // writing full pages of 4 bytes each
                    for (int i = 0; i < dataPages; i++) {
                        System.out.println("starting round: " + i);
                        byte[] commandW;
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + i) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (i * 4)],
                                dataByte[1 + (i * 4)],
                                dataByte[2 + (i * 4)],
                                dataByte[3 + (i * 4)]
                        };
                        nfcaContent = nfcaContent + "command: " + Utils.bytesToHex(commandW) + "\n";
                        response = nfcA.transceive(commandW);
                        if (response == null) {
                            // either communication to the tag was lost or a NACK was received
                            // Log and return
                            nfcaContent = nfcaContent + "ERROR: null response";
                            String finalNfcaText = nfcaContent;
                            writeToUiAppend(nfcResult, finalNfcaText);
                            return;
                        } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                            // NACK response according to Digital Protocol/T2TOP
                            // Log and return
                            nfcaContent = nfcaContent + "ERROR: NACK response: " + Utils.bytesToHex(response);
                            String finalNfcaText = nfcaContent;
                            writeToUiAppend(nfcResult, finalNfcaText);
                            return;
                        } else {
                            // success: response contains ACK or actual data
                            // copy the response to the ntagMemory
                            nfcaContent = nfcaContent + "response:\n" + Utils.bytesToHex(response) + "\n";
                            //System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * i), nfcaMaxTranceive4ByteLength);
                        }
                    }

                    // ### section for writing only a part of page
                    if (dataPagesMod == 0) {
                        // don't write a new page
                        nfcaContent = nfcaContent + "write result: SUCCESS" + "\n";
                        writeToUiAppend(nfcResult, nfcaContent);
                        try {
                            nfcA.close();
                        } catch (IOException e) {
                            writeToUiAppend(nfcResult, "IOException: " + e);
                        }
                        return;
                    }
                    byte[] commandW = new byte[0];
                    if (dataPagesMod == 1) {
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + dataPages) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (dataPages * 4)],
                                (byte) 0x00,
                                (byte) 0x00,
                                (byte) 0x00
                        };
                    }
                    if (dataPagesMod == 2) {
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + dataPages) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (dataPages * 4)],
                                dataByte[1 + (dataPages * 4)],
                                (byte) 0x00,
                                (byte) 0x00
                        };
                    }
                    if (dataPagesMod == 3) {
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + dataPages) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (dataPages * 4)],
                                dataByte[1 + (dataPages * 4)],
                                dataByte[2 + (dataPages * 4)],
                                (byte) 0x00
                        };
                    }

                    nfcaContent = nfcaContent + "command: " + Utils.bytesToHex(commandW) + "\n";
                    response = nfcA.transceive(commandW);
                    if (response == null) {
                        // either communication to the tag was lost or a NACK was received
                        // Log and return
                        nfcaContent = nfcaContent + "ERROR: null response";
                        String finalNfcaText = nfcaContent;
                        writeToUiAppend(nfcResult, finalNfcaText);
                        return;
                    } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                        // NACK response according to Digital Protocol/T2TOP
                        // Log and return
                        nfcaContent = nfcaContent + "ERROR: NACK response: " + Utils.bytesToHex(response);
                        String finalNfcaText = nfcaContent;
                        writeToUiAppend(nfcResult, finalNfcaText);
                        return;
                    } else {
                        // success: response contains ACK or actual data
                        // copy the response to the ntagMemory
                        //nfcaContent = nfcaContent + "number of bytes read: : " + response.length + "\n";
                        nfcaContent = nfcaContent + "response:\n" + Utils.bytesToHex(response) + "\n";
                        //System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * i), nfcaMaxTranceive4ByteLength);
                    }

                } catch(TagLostException e){
                    // Log and return
                    System.out.println("ERROR: Tag lost exception in body of WriteAuth");
                    nfcaContent = nfcaContent + "ERROR: Tag lost exception";
                    String finalNfcaText = nfcaContent;
                    writeToUiAppend(nfcResult, finalNfcaText);
                    return;
                } catch(IOException e){
                    writeToUiAppend(nfcResult, "WIOException: " + e + "\nMaybe the tag is write protected ?");
                    e.printStackTrace();
                    return;
                }
                nfcaContent = nfcaContent + "write result: SUCCESS" + "\n";
                String finalNfcaRawText = nfcaContent;
                String finalNfcaText = "parsed content:\n" + new String(ntagMemory, StandardCharsets.US_ASCII);
                writeToUiAppend(nfcResult, finalNfcaRawText);
                writeToUiAppend(nfcResult, finalNfcaText);
            } else{
                writeToUiToast("NFC tag is NOT Nfca compatible");
                return;
            }
        } catch ( IOException e)
        {
            //Trying to catch any ioexception that may be thrown
            e.printStackTrace();
            writeToUiAppend(nfcResult, "XIOException: " + e);
            return;
        } catch ( Exception e)
        {
            //Trying to catch any exception that may be thrown
            e.printStackTrace();
            writeToUiAppend(nfcResult, "Exception: " + e);
            return;
        } finally
        {
            try {
                nfcA.close();
            } catch (IOException e) {
                writeToUiAppend(nfcResult, "YIOException: " + e);
            }
        }
    }

    private byte[] sendPwdAuthData(NfcA nfcA, byte[] passwordByte, TextView textView) {
        byte[] response; // the response is the PACK returned by the tag when successful authentication
        byte[] command = new byte[]{
                (byte) 0x1B,  // PWD_AUTH
                passwordByte[0],
                passwordByte[1],
                passwordByte[2],
                passwordByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "ERROR: null response");
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "ERROR: NACK response: " + Utils.bytesToHex(response));
                return null;
            } else {
                // success: response contains (P)ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + Utils.bytesToHex(response));
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception OR Tag is not protected");
            return null;
        } catch (IOException e) {
            writeToUiAppend(textView, "IOException: " + e.toString());
            e.printStackTrace();
            return null;
        }
        return response;
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

    private void showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mRead = menu.findItem(R.id.action_read);
        mRead.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteActivity.this, NtagDataReadingActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteProtection = menu.findItem(R.id.action_write_protection);
        mWriteProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteActivity.this, SetWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mRemoveProtection = menu.findItem(R.id.action_remove_protection);
        mRemoveProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mSpecialSettings = menu.findItem(R.id.action_special_settings);
        mSpecialSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteActivity.this, SpecialSettingsActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteNdef = menu.findItem(R.id.action_write_ndef_message);
        mWriteNdef.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteActivity.this, WriteNdefMessageActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mEnableMirrorNdefMessage = menu.findItem(R.id.action_enable_ndef_message_mirror);
        mEnableMirrorNdefMessage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteActivity.this, EnableMirrorForNdefActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mClearTag = menu.findItem(R.id.action_clear_tag);
        mClearTag.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteActivity.this, ClearTagActivity.class);
                startActivity(i);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

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