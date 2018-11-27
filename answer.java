package com.example.sinn.applicationhci;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class answer extends Activity {
    private String username, qname, qcontent,problem,answercontent, isAudio, docu;
    private TextView textView1, textView2;
    private Button button,play,audio;

    private ListView listView;
    private answerAdapter answerAdapter;
    private int frequency = 11025;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int audiosource = MediaRecorder.AudioSource.MIC;
    int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    File recordingFile = null;
    answer.PlayAudio playAudio = null;
    private boolean isPlaying = false;
    Vibrator vibrator;
    private answer.ReceiveBroadCast receiveBroadCast;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.answer );
        Intent intent = getIntent();
        username = intent.getStringExtra( "username" );
        qname = intent.getStringExtra( "name" );
        problem = intent.getStringExtra( "problem" );
        qcontent = intent.getStringExtra( "content" );
        isAudio = intent.getStringExtra("isAudio");
        docu = intent.getStringExtra("docu");
        textView1 = (TextView) findViewById( R.id.answer_name );
        textView2 = (TextView) findViewById( R.id.answer_content );
        final EditText editText= (EditText) findViewById( R.id.answer );
        button = (Button) findViewById( R.id.answer_pub );
        play = (Button) findViewById(R.id.play_question);
        audio = (Button) findViewById(R.id.audio_answer);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if(isAudio.equals("true")){
            textView1.setText( qname );
            textView2.setText( "this is an Audio" );
        }else{
        textView1.setText( qname );
        textView2.setText( qcontent );
        play.setVisibility(View.INVISIBLE);
        }
        initBroadCast();
        editText.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (editText.length() > 0) {
                    button.setEnabled( true );
                } else {
                    button.setEnabled( false );
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        } );

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(30);
                final ProgressDialog dialog = new ProgressDialog(answer.this);

                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);



                dialog.setMessage("waitting.....");

                dialog.setCancelable(false);

                dialog.show();

                String url =qcontent;
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference httpsReference = storage.getReferenceFromUrl( url );
                try {
                    recordingFile = File.createTempFile("recording", ".pcm");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                httpsReference.getFile(recordingFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        dialog.cancel();
                        playRecorder();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                    }
                });
            }
        });

        button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date date = new Date(  );
                answercontent=editText.getText().toString();
                if ( answercontent!= null) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put( "answer", editText.getText().toString() );
                    map.put( "from",username );
                    map.put( "Date",date );
                    map.put( "isAudio","false" );
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection( "questions" ).document(problem).collection("questions").document(docu).collection( "answers" ).document(answercontent)
                            .set( map )
                            .addOnSuccessListener( new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    AlertDialog alertDialog = new AlertDialog.Builder(answer.this).create();
                                    alertDialog.setTitle( "Alert" );
                                    alertDialog.setMessage( "successfully answer" );
                                    alertDialog.setButton( DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            InputMethodManager imm = (InputMethodManager) getSystemService(question.INPUT_METHOD_SERVICE);
                                            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                                            inidata();
                                        }
                                    } );
                                    alertDialog.show();
                                }
                            } )
                            .addOnFailureListener( new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            } );
                    editText.setText( "" );
                }else {
                    AlertDialog alertDialog = new AlertDialog.Builder(answer.this).create();
                    alertDialog.setTitle( "Alert" );
                    alertDialog.setMessage( "you need to answer question first" );
                    alertDialog.setButton( DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {


                        }
                    } );
                    alertDialog.show();
                }

            }

        } );

        audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( answer.this, recoder.class );
                Bundle bundle = new Bundle();
                bundle.putString( "isanswer","true" );
                bundle.putString( "username", username );
                bundle.putString( "name", qname );
                bundle.putString( "docu", docu );
                bundle.putString( "problem", problem);
                intent.putExtras( bundle );
                startActivity( intent );
            }
        });

        inidata();
    }
    private void inidata() {
        final List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection( "questions" ).document(problem).collection("questions").document(docu).collection( "answers" )
                .orderBy( "Date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                ;
                                Map<String, Object> map = new HashMap<String, Object>();
                                map.putAll( document.getData() );
                                listItems.add( map );
                            }
                            iniUI(listItems);
                        } else {

                        }
                    }
                } );
    }
    private void iniUI( List<Map<String, Object>> listItems){
        listView=(ListView)findViewById( R.id.ans_list );
        answerAdapter= new answerAdapter(listItems,this);
        listView.setAdapter( answerAdapter );
    }

    public void playRecorder() {

        playAudio = new answer.PlayAudio();
        playAudio.execute();
    }
    private class PlayAudio extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            isPlaying = true;
            int bufferSize = AudioTrack.getMinBufferSize(frequency, channelConfig, audioFormat);
            short[] buffer = new short[bufferSize / 4];
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordingFile)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
            try {
                while (isPlaying && dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < buffer.length) {
                        buffer[i] = dis.readShort();
                        i++;
                    }
                    audioTrack.write(buffer, 0, buffer.length);
                }
                dis.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Toast.makeText(answer.this, "start playing", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(answer.this, "done", Toast.LENGTH_SHORT).show();

        }
    }
    class ReceiveBroadCast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            inidata();
        }
    }
    public void initBroadCast() {
        receiveBroadCast = new answer.ReceiveBroadCast();
        IntentFilter filter = new IntentFilter();
        filter.addAction( "updateanswer" );
        registerReceiver( receiveBroadCast, filter );
    }
    public void onDestroy() {
        unregisterReceiver( receiveBroadCast );
        super.onDestroy();
    }

}