package com.thankcreate.care.account;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.markupartist.android.widget.ActionBar;
import com.thankcreate.care.App;
import com.thankcreate.care.AppConstants;
import com.thankcreate.care.R;
import com.thankcreate.care.R.layout;
import com.thankcreate.care.R.menu;
import com.thankcreate.care.account.AccountActivity.AccountGroupAdapter.ViewHolder;
import com.thankcreate.care.control.SearchBarWidget;
import com.thankcreate.care.control.SearchBarWidget.onSearchListener;
import com.thankcreate.care.tool.converter.SinaWeiboConverter;
import com.thankcreate.care.tool.misc.FirstCharactorComparator;
import com.thankcreate.care.tool.misc.MiscTool;
import com.thankcreate.care.tool.misc.PreferenceHelper;
import com.thankcreate.care.tool.misc.StringTool;
import com.thankcreate.care.tool.ui.DrawableManager;
import com.thankcreate.care.tool.ui.ToastHelper;
import com.thankcreate.care.viewmodel.EntryType;
import com.thankcreate.care.viewmodel.FriendViewModel;
import com.thankcreate.care.viewmodel.SimpleTableModel;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.FriendshipsAPI;
import com.weibo.sdk.android.net.RequestListener;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

public class AccountSelectFreindActivity extends Activity {

	private ActionBar actionBar;
	private SearchBarWidget searchBarWidget;
	private LinearLayout progressLinearLayout;
	private ListView listViewFriend;
	private EditText txtInput;
	private FriendListAdapter adapter;
	
	private int type;
	private List<FriendViewModel> listFriendsInShow = new ArrayList<FriendViewModel>();
	private List<FriendViewModel> listFriendsAll = new ArrayList<FriendViewModel>();
	private DrawableManager drawableManager = new DrawableManager();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account_select_freind);
		
		initControl();
		initActionBar();
		parseIntent();
		loadFriendList();
	}

	private void initControl()
	{
		actionBar = (ActionBar) findViewById(R.id.actionbar);
		searchBarWidget = (SearchBarWidget) findViewById(R.id.search_bar);
		progressLinearLayout = (LinearLayout) findViewById(R.id.progess);
		txtInput = (EditText) findViewById(R.id.search_text);
		listViewFriend =  (ListView) findViewById(R.id.listView1);
		listViewFriend.setFastScrollEnabled(true);		
		
		searchBarWidget.setOnSearchListener(new onSearchListener() {			
			@Override
			public void onSearchChange(String key) {
				search(key);
			}
		});
		
		listViewFriend.setOnScrollListener(onScrollListener);
		listViewFriend.setOnItemClickListener(onItemClickListener);
	}
			
	private void initActionBar()
	{		
        actionBar.setTitle("选择关注人");       
        actionBar.SetTitleLogo(R.drawable.tab_account);
	}
	
	private void parseIntent()
	{
		Intent it=this.getIntent();
		type = (it.getIntExtra("type", EntryType.NotSet));
	}

	private OnScrollListener onScrollListener = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {	
			InputMethodManager imm = (InputMethodManager)getSystemService(
				      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(txtInput.getWindowToken(), 0);
			txtInput.clearFocus();
			txtInput.setFocusable(false);
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {		
		}
	};
	
	private OnItemClickListener onItemClickListener = new OnItemClickListener()
	{

		@Override
		public void onItemClick(AdapterView<?> arg0,  View view, int position,
				long id) {
			if(position < 0 || position >= listFriendsInShow.size())
				return;
			FriendViewModel friend = listFriendsInShow.get(position);
			
			SharedPreferences pref = AccountSelectFreindActivity.this.getSharedPreferences(
					AppConstants.PREFERENCES_NAME, Context.MODE_APPEND);
			Editor editor = pref.edit();
			editor.putString("SinaWeibo_FollowerID", friend.ID);
			editor.putString("SinaWeibo_FollowerNickName", friend.name);
			editor.putString("SinaWeibo_FollowerAvatar", friend.avatar);
			editor.putString("SinaWeibo_FollowerAvatar2", friend.avatar2);
			editor.commit();
			App.mainViewModel.isChanged = true;
			finish();
			
		}			
	};
	
	private void search(String key)
	{
		if(listFriendsAll == null || listFriendsAll.size() ==0)
			return;
		
		if(StringTool.isNullOrEmpty(key))
		{
			listFriendsInShow = listFriendsAll;		
			adapter.setListModel(listFriendsInShow);
			
			listViewFriend.post(new Runnable() {					
				@Override
				public void run() {					
					
					listViewFriend.setAdapter(adapter);
				}
			});		
		}
		
		List<FriendViewModel> searchResult = new ArrayList<FriendViewModel>();		
		for (FriendViewModel friendViewModel : listFriendsAll) {
			if(friendViewModel.name.toLowerCase().contains(key.toLowerCase()))
			{
				searchResult.add(friendViewModel);
			}
		}
		
		listFriendsInShow = searchResult;		
		adapter.setListModel(listFriendsInShow);
		
		listViewFriend.post(new Runnable() {					
			@Override
			public void run() {					
				
				listViewFriend.setAdapter(adapter);
			}
		});			
		
	}
	
	private void loadFriendList()
	{ 
		listFriendsAll.clear();
		
		if(type == EntryType.SinaWeibo)
		{
			loadFriendSinaWeibo();
		}
		else if(type == EntryType.Renren)
		{
			// TODO
		}
		else if(type == EntryType.Douban)
		{
			// TODO
		}
		
	}
	
	private void loadFriendSinaWeibo()
	{
		
		Oauth2AccessToken oa = MiscTool.getOauth2AccessToken();
		if(oa == null)
			return;
		FriendshipsAPI friendshipsAPI = new FriendshipsAPI(oa);
		String myId = MiscTool.getCurrentAccountID(EntryType.SinaWeibo);
		friendshipsAPI.friends(Long.parseLong(myId), 200, 0, false, mSinaWeiboShowFriendsRequestListener);
	}
	
	RequestListener mSinaWeiboShowFriendsRequestListener = new RequestListener()
	{

		@Override
		public void onComplete(String arg0) {
			try {
				JSONObject jsonObject = new JSONObject(arg0);
				int nextCursorString = jsonObject.getInt("next_cursor");
				JSONArray users = jsonObject.getJSONArray("users");
				if(users == null )
					return;
				int length = users.length();
				for (int i = 0 ; i< length; i++) {
					JSONObject user = users.getJSONObject(i);
					FriendViewModel model = SinaWeiboConverter.convertFriendToCommon(user);
					if(model != null)
					{
						listFriendsAll.add(model);
					}
				}

				// 不为0，继续请求
				if (nextCursorString != 0) {
					Oauth2AccessToken oa = MiscTool
							.getOauth2AccessToken();
					if(oa == null)
						return;
					FriendshipsAPI friendshipsAPI = new FriendshipsAPI(oa);
					String myId = MiscTool.getCurrentAccountID(EntryType.SinaWeibo);
					friendshipsAPI.friends(Long.parseLong(myId),
							200, nextCursorString, false,
							mSinaWeiboShowFriendsRequestListener);
				}
				// 为0，刷新页面
				else {
					fetchComplete();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}

		@Override
		public void onError(WeiboException arg0) {
			ToastHelper.show("获取朋友列表过程中发生未知错误，请确保网络通畅");
		}

		@Override
		public void onIOException(IOException arg0) {
			ToastHelper.show("获取朋友列表过程中发生未知错误，请确保网络通畅");			
		}
		
	};

	public void fetchComplete()
	{
		Collections.sort(listFriendsAll, new FirstCharactorComparator());
		listFriendsInShow.addAll(listFriendsAll);		

		adapter = new FriendListAdapter(this);
		adapter.setListModel(listFriendsInShow);
		
		listViewFriend.post(new Runnable() {					
			@Override
			public void run() {					
				progressLinearLayout.setVisibility(View.GONE);
				listViewFriend.setAdapter(adapter);
			}
		});			
		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_account_select_freind, menu);
		return true;
	}
	
	
	class FriendListAdapter extends BaseAdapter implements SectionIndexer{

		private List<FriendViewModel> listModel = new ArrayList();;
		private LayoutInflater mInflater;
		private String mSections = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		
		public FriendListAdapter(Context context) {
			super();
			mInflater = LayoutInflater.from(context);
		}
		
		public void addItem(FriendViewModel model) {
			listModel.add(model);
			notifyDataSetChanged();
		}
		
		public void setListModel(List<FriendViewModel> input){
			listModel = input;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return listModel.size();
		}

		@Override
		public Object getItem(int position) {
			try {
				return listModel.get(position);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {	
			ViewHolder holder = null;
			if(convertView == null)
			{
				holder = new ViewHolder();
				
				convertView = mInflater.inflate(R.layout.listview_item_friend, null);
				holder.textName = (TextView) convertView.findViewById(R.id.friend_list_item_name);	
				holder.textDescription = (TextView) convertView.findViewById(R.id.friend_list_item_description);
				holder.imageAvatar = (ImageView) convertView.findViewById(R.id.friend_list_item_avatar);
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder)convertView.getTag();				
			}
			
			holder.textName.setText(listModel.get(position).name);
			holder.textDescription.setText(listModel.get(position).description);
			holder.imageAvatar.setTag(listModel.get(position).avatar);
			drawableManager.fetchDrawableOnThread(listModel.get(position).avatar, holder.imageAvatar);
			return convertView;
		}
		
		public class ViewHolder {
	        public ImageView imageAvatar;
	        public TextView textName;
	        public TextView textDescription;
	        public int tag;
	    }

		@Override
		public int getPositionForSection(int section) {			
			try {
				for (int i = section; i >= 0; i--) {
					for (int j = 0; j < getCount(); j++) {
						if (i == 0) {
							// For numeric section
							for (int k = 0; k <= 9; k++) {
								if (listModel.get(j).firstCharactor.equalsIgnoreCase(String.valueOf(k)))
									return j;
							}
						} else {
							if (listModel.get(j).firstCharactor.equalsIgnoreCase(String.valueOf(mSections.charAt(i))))
								return j;
						}
					}
				}
			} catch (Exception e) {
				return 0;
			}			
			return 0;
		}

		@Override
		public int getSectionForPosition(int position) {
			return 0;
		}

		@Override
		public Object[] getSections() {
			String[] sections = new String[mSections.length()];
			for (int i = 0; i < mSections.length(); i++)
				sections[i] = String.valueOf(mSections.charAt(i));
			return sections;
		}

	}
	

}
