package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    //variabili momentanee
    EditText mResultText;
    ImageView mPreviewIv;

    Button btnfetch;
    ListView listview;

    String value;


    private static final int STORAGE_REQUEST_CODE = 301;
    private static final int IMAGE_PICK_GALLERY_CODE = 302;

    String storagePermission[];

    //riferimento immutabile
    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle("Aggiungi un'immagine da verificare ->");

        mResultText = findViewById(R.id.resultText); //si rifà all'id in res -> layout -> activity_main
        mPreviewIv = findViewById(R.id.imageIv);
        btnfetch = (Button) findViewById(R.id.buttonfetch);
        listview = (ListView)findViewById(R.id.listView);

        btnfetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getData();
            }
        });


        //storage permission
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    //barra strumenti menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //selezione opzione nel menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        //controllo l'id selezionato
        if (id == R.id.addImage) {
            if (!checkStoragePermission()) {
                requestStoragePermissions();
            } else {
                //work ->
                pickGallery();
            }
        }
        if (id == R.id.settings) {
            //con Toast si richiamo i messaggi a schermo di andriod
            Toast.makeText(this, "Impostazioni", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }


    private void pickGallery() {
        //Intent per prendere un'immagine dalla galleria
        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE); //da qui si attiva riga 124
    }

    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    //risultati dei permessi, verrà usata quando in un primo momento non vi sono i permessi, per poi arrivare sempre al pickGallery
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (writeStorageAccepted) {
                    pickGallery();
                } else {
                    Toast.makeText(this, "Permesso Negato", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    //immagine riportata
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON) //abilito le lineeguida per l'immagine
                        .start(this);
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult res = CropImage.getActivityResult(data); //in res ottengo il risultato dell'activity del crop

            if (resultCode == RESULT_OK) {
                Uri resUri = res.getUri(); //get uri dell'immagine (come l'URL)
                mPreviewIv.setImageURI(resUri); //pongo l'immagine sul campo preview

                //un bitmap è un immagine come un Jpeg o un png, un drawable è un oggetto astratto "disegnabile"

                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable(); //ottengo l'immagine in drawable (tipo di immagine android
                Bitmap bitmap = bitmapDrawable.getBitmap(); //e trasformo in bitmap

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build(); //applico l'ocr

                if (!recognizer.isOperational()) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                } else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build(); //dati meta dell'immagine
                    SparseArray<TextBlock> items = recognizer.detect(frame); //estraggo il testo
                    StringBuilder sb = new StringBuilder(); //creo la stringa che ospiterà il risultato

                    //in items si ottiene il testo
                    //nel seguente for questo succede finchè non vi sarà testo (controllo continuo)

                    for (int i = 0; i < items.size(); i++) {
                        TextBlock block = items.valueAt(i);
                        //aggiungo alla stringa
                        sb.append(block.getValue());
                        sb.append("\n");
                    }

                    //setto il testo acquisito nel editText
                    mResultText.setText(sb.toString());
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = res.getError();
                Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getData() {

        value = removeStopWords();

        System.out.println("ELEM TO SEARCH IN PHP :-"+value+"-");

        if (value.equals("")) {
            Toast.makeText(this, "Aggiungi prima un immagine valida", Toast.LENGTH_LONG).show();
            return;
        }
        String url = DataNews.DATA_URL + value;
        //qua formo la stringa di richiesta

        StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                showJSON(response); //si passa alla funzione successiva
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
        });

        //le richieste volley sono incluse nella omonima tealibreria HTTP che migliora la  pianificazione della richiesta di rete
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void showJSON(String response) { //a questa funzione arriva la risposta della richiesta
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray result = jsonObject.getJSONArray(DataNews.JSON_ARRAY);
            for (int i = 0; i < result.length(); i++) {
                JSONObject jo = result.getJSONObject(i);
                String title = jo.getString(DataNews.KEY_TITLE);
                String date = jo.getString(DataNews.KEY_DATE);
                String data = jo.getString(DataNews.KEY_DATA);
                String id = jo.getString(DataNews.KEY_ID);

                final HashMap<String, String> news_find = new HashMap();
                news_find.put(DataNews.KEY_TITLE, title);
                news_find.put(DataNews.KEY_DATE, "Articolo del: " +date);
                news_find.put(DataNews.KEY_DATA, data);
                news_find.put(DataNews.KEY_ID, id);

                list.add(news_find);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ListAdapter adapter = new SimpleAdapter(
                MainActivity.this, list, R.layout.activity_list,
                new String[]{DataNews.KEY_TITLE, DataNews.KEY_DATE, DataNews.KEY_DATA, DataNews.KEY_ID},
                new int[]{R.id.title, R.id.date, R.id.data, R.id.tvid});

        listview.setAdapter(adapter);
    }


    public String removeStopWords(){

        value = mResultText.getText().toString().trim();

        // System.out.println("Frase iniziale: "+value);

        String[] arrayStopWords = StopWords.STOP_WORDS.split(" "); //lista degli elementi da rimuovere
        String result = new String();

        result=" ";

        String[] split = value.split("[ \n]");



        List<String> listSplit = new ArrayList<String>(Arrays.asList(split));
        //System.out.println("888888:"+listSplit+"---");


        //IN QUESTO FOR VI è LA RIMOZIONE DEGLI ELEMENTI INUTILE, LEVANDO GLI ELEMENTI DI LISTA stopward.java, e levando a capo e spazi residui
        for(int i = 0; i< arrayStopWords.length; i++){

            for(int j = 0; j< listSplit.size(); j++){
                listSplit.set(j,listSplit.get(j).toLowerCase()); //tutto minuscolo
                listSplit.set(j, listSplit.get(j).trim()); //eseguo il trim per ogni parola
                //System.out.println("001002:"+listSplit+"---");
               // System.out.println("-"+arrayStopWords[i] + "- =? -"+listSplit.get(j).toString()+"-");

                if(listSplit.get(j).toString().compareTo(arrayStopWords[i]) == 0){
                    System.out.println("parola da rimuovere");
                    listSplit.set(j, "");

                }

                if(listSplit.get(j).toString() == "\n" || listSplit.get(j).toString() == " "){
                    System.out.println("testo a capo da rimuovere");
                    listSplit.set(j, "");
                }
            }
        }

        split = listSplit.toArray(new String[0]);

        for(int y=0; y<split.length;y++){
            //System.out.println("Il risultato è: "+split[y]);
            result += split[y] + " ";
        }

        //System.out.println("Il risultato è: "+result);

        return result.trim();
    }

}