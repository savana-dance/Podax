package com.axelby.podax.ui;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MainActivity extends SherlockFragmentActivity {

	public static final int TAB_QUEUE = 0;
	public static final int TAB_SUBSCRIPTIONS = 1;
	public static final int TAB_DOWNLOADS = 2;
	public static final int TAB_ABOUT = 3;

	protected int _focusedPage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// check if this was opened by android to save an RSS feed
		Intent intent = getIntent();
		if (intent.getDataString() != null) {
			ContentValues values = new ContentValues();
			values.put(SubscriptionProvider.COLUMN_URL, intent.getDataString());
			Uri savedSubscription = getContentResolver().insert(SubscriptionProvider.URI, values);
			UpdateService.updateSubscription(this, Integer.valueOf(savedSubscription.getLastPathSegment()));
		}

		// clear RSS error notification
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		TabsAdapter tabsAdapter = new TabsAdapter(getSupportFragmentManager());
		pager.setAdapter(tabsAdapter);

		TitlePageIndicator titleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
		titleIndicator.setViewPager(pager);
		titleIndicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				_focusedPage = position;
				invalidateOptionsMenu();
			}
		});

		if (savedInstanceState != null) {
			_focusedPage = savedInstanceState.getInt("focusedPage", 0);
			titleIndicator.setCurrentItem(_focusedPage);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("focusedPage", _focusedPage);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    switch (_focusedPage) {
	    case 1:
	    	inflater.inflate(R.menu.subscriptionlist_activity, menu);
	    	return true;
	    case 2:
	    	inflater.inflate(R.menu.downloadlist_activity, menu);
	    	return true;
	    }
	    return false;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_subscription:
			startActivity(new Intent(this, AddSubscriptionActivity.class));
			return true;
		case R.id.restart:
			UpdateService.downloadPodcasts(this);
			return true;
		case R.id.preferences:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.refresh_subscriptions:
			UpdateService.updateSubscriptions(this);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public class TabsAdapter extends FragmentStatePagerAdapter
		implements TitleProvider
	{

		private String[] _titles;

		public TabsAdapter(FragmentManager fm) {
			super(fm);

			Resources resources = getResources();
			_titles = new String[] {
					resources.getString(R.string.queue),
					resources.getString(R.string.subscriptions),
					resources.getString(R.string.downloads),
					resources.getString(R.string.about)
			};
		}

		@Override
		public Fragment getItem(int item) {
			switch (item) {
			case TAB_QUEUE:
				return new QueueFragment();
			case TAB_SUBSCRIPTIONS:
				return new SubscriptionListFragment();
			case TAB_DOWNLOADS:
				return new ActiveDownloadListFragment();
			case TAB_ABOUT:
				return new AboutFragment();
			}
			throw new IllegalArgumentException();
		}

		@Override
		public String getTitle(int position) {
			return _titles[position].toUpperCase();
		}

		@Override
		public int getCount() {
			return 4;
		}
	}

	public static Intent getSubscriptionIntent(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(Constants.EXTRA_TAB, MainActivity.TAB_SUBSCRIPTIONS);
		return intent;
	}
}