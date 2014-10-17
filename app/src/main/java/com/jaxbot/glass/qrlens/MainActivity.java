package com.jaxbot.glass.qrlens;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.jaxbot.glass.barcode.scan.CaptureActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
	final int SCAN_QR = 4;
    final String TAG = "app";

    final String FOOTER = "QR text content";

    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private MyCardScrollAdapter mAdapter;

    boolean mNeedsReadMore;

    String mCardData;

    Context context;

    boolean allowDestroy = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, CaptureActivity.class);
        startActivityForResult(intent, SCAN_QR);

        context = this;
	}

    @Override
    protected void onPause()
    {
        super.onPause();
        if (allowDestroy)
            finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SCAN_QR) {
			if (resultCode == RESULT_OK) {
				Bundle res = data.getExtras();

                String qrtype = res.getString("qr_type");
                String qrdata = res.getString("qr_data");

				Log.w(TAG, qrtype);
				Log.w(TAG, qrdata);

				if (qrtype.equals("URI")) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(qrdata));
					startActivity(browserIntent);

					finish();
				} else {

                    createCards(qrdata);

                    mCardScrollView = new CardScrollView(this);
                    mAdapter = new MyCardScrollAdapter();
                    mCardScrollView.setAdapter(mAdapter);
                    mCardScrollView.activate();

                    mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                            if (mNeedsReadMore) {
                                audio.playSoundEffect(Sounds.TAP);
                                openOptionsMenu();
                            } else {
                                audio.playSoundEffect(Sounds.DISALLOWED);
                            }
                        }
                    });
                    setContentView(mCardScrollView);

                    allowDestroy = true;
				}
			} else {
                finish();
            }
		}
	}

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.readmore, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menu_item_1:
                createCardsPaginated();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createCards(String data) {
        mCardData = data;
        mCards = new ArrayList<CardBuilder>();

        if (data.length() > 225 || data.split("\\n").length > 7)
            mNeedsReadMore = true;

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
            .setText(data)
            .setFootnote(FOOTER)
        );

    }

    int numNewlines(String str)
    {
        Matcher m = Pattern.compile("(\n)|(\r)|(\r\n)").matcher(str);
        int lines = 0;
        while (m.find())
        {
            lines ++;
        }
        return lines;
    }

    private void createCardsPaginated() {
        mCards = new ArrayList<CardBuilder>();

        String[] chunks = mCardData.split("\\b");

        int lines = 0;
        String line = "";

        for (int i = 0; i < chunks.length; i++) {
            String hunk = "";
            line = "";
            lines = 0;
            for (; i < chunks.length; i++) {
                if ((line + chunks[i]).length() > 23) {
                    Log.i("Lines overflow", line);
                    line = "";
                    lines++;
                }
                line += chunks[i];
                if (numNewlines(chunks[i]) > 0)
                {
                    Log.i("Lines newlines", line);
                    Log.i("Lines", "newlines");
                    line = "";
                    lines++;
                }
                if (lines > 6)
                {
                    Log.i("Lines", "7");
                    i--;
                    break;
                }
                hunk += chunks[i];

                Log.i("Hunk", hunk);
            }
            if (hunk.substring(0, 2).equals("\r\n"))
                hunk = hunk.substring(2);
            if (hunk.substring(0, 1).equals(" ") || hunk.substring(0, 1).equals("\n") || hunk.substring(0, 1).equals("\r"))
                hunk = hunk.substring(1);
            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT_FIXED)
                            .setText(hunk)
            );
        }

        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();

        mNeedsReadMore = false;
    }

    private class MyCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position){
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
}

