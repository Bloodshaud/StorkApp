package stork.dk.storkapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import stork.dk.storkapp.adapter.Friend;
import stork.dk.storkapp.adapter.Group;
import stork.dk.storkapp.R;
import stork.dk.storkapp.communicationObjects.ChangeGroupRequest;
import stork.dk.storkapp.communicationObjects.CommunicationsHandler;
import stork.dk.storkapp.communicationObjects.Constants;
import stork.dk.storkapp.communicationObjects.GroupChangeActivationRequest;

/**
 * @author Mathias
 */

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listHashMap;
    private SharedPreferences sharedPref;
    private HashMap<Integer, Integer> groupIdsAndPos;
    private List<List> groupsFromResp;
    private int userId;
    private String sessionId;

    public ExpandableListAdapter(Context context, List<String> listDataHeader, HashMap<String, List<String>> listHashMap, HashMap<Integer, Integer> groupIdsAndPos, List<List> groupsFromResp) {
        this.context = context;
        this.listDataHeader = listDataHeader;
        this.listHashMap = listHashMap;
        this.groupIdsAndPos = groupIdsAndPos;
        this.groupsFromResp = groupsFromResp;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public int getGroupCount() {
        return listDataHeader.size();
    }

    @Override
    public int getChildrenCount(int i) {
        if (listHashMap != null) {
            return listHashMap.get(listDataHeader.get(i)).size();
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listDataHeader.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return listHashMap.get(listDataHeader.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View view, ViewGroup parent) {
        final ExpandableListView elv = (ExpandableListView) parent;
        sharedPref = context.getSharedPreferences(Constants.APP_SHARED_PREFS, Context.MODE_PRIVATE);
        sessionId = sharedPref.getString(Constants.CURRENT_SESSION_KEY, "");
        userId = sharedPref.getInt(Constants.CURRENT_USER_KEY, 0);

        String headerTitle = (String) getGroup(groupPosition);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater != null ? inflater.inflate(R.layout.expandable_listview_group, null) : null;
        }

        TextView editGroupButton = (TextView) (view != null ? view.findViewById(R.id.editGroup) : null);
        if (editGroupButton != null) {
            editGroupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Group group = (Group) groupsFromResp.get(0).get(groupPosition);

                    ChangeGroupRequest req = new ChangeGroupRequest();
                    req.setId(groupIdsAndPos.get(groupPosition));
                    req.setSessionId(sessionId);
                    req.setName(group.getName() + "1");
                    req.setUserId(userId);
                    List<Integer> friendsToRemove = new ArrayList<>();
                    for (Friend f : group.getFriends()) {
                        friendsToRemove.add(f.getId());
                    }
                    friendsToRemove.add(userId);
                    req.setRemove(friendsToRemove);
                    Log.d("LOG" + "id:", groupIdsAndPos.get(groupPosition).toString());
                    Log.d("LOG" + "sessionId:", sessionId);
                    Log.d("LOG" + "group name:", req.getName());
                    Log.d("LOG" + "friends:", friendsToRemove.toString());
                    Log.d("LOG" + "list:", req.getRemove().toString());


                    CommunicationsHandler.changeGroup(context, req, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d("LOG statuscode",statusCode + "");
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.d("LOG statuscode",statusCode + "");
                        }
                    });
                }
            });
        }

        TextView listHeader = (TextView) (view != null ? view.findViewById(R.id.lblListHeader) : null);
        if (listHeader != null) {
            listHeader.setTypeface(null, Typeface.BOLD);
            listHeader.setText(headerTitle);
            listHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isExpanded) {
                        elv.expandGroup(groupPosition);
                    } else {
                        elv.collapseGroup(groupPosition);
                    }
                }
            });
        }


        final Switch switchBtn = (Switch) (view != null ? view.findViewById(R.id.headerSwitch) : null);
        //TODO:
        //If groups.getLocation != null
        //switchBtn.setChecked()
        if (switchBtn != null) {

            switchBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        System.out.println("ON");
                        changeGroupActivation(true, groupPosition);
                    } else {
                        System.out.println("OFF");
                        changeGroupActivation(false, groupPosition);
                    }
                }
            });
        }

        return view;
    }

    private void changeGroupActivation(Boolean activation, Integer pos) {
        List<Integer> groupToChange = new ArrayList<>();


        groupToChange.add(groupIdsAndPos.get(pos));

        GroupChangeActivationRequest req = new GroupChangeActivationRequest();
        req.setSessionId(sessionId);
        req.setUserId(userId);

        if (activation) {
            req.setActivateGroup(groupToChange);
        } else {
            req.setDeactivateGroup(groupToChange);
        }


        CommunicationsHandler.changeGroupActivation(context, req, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
        final String childText = (String) getChild(groupPosition, childPosition);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater != null ? inflater.inflate(R.layout.expandable_listview_child, null) : null;
        }

        TextView textChild = (TextView) (view != null ? view.findViewById(R.id.lblListChild) : null);
        if (textChild != null) {
            textChild.setText(childText);
        }

        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {

    }

    @Override
    public void onGroupCollapsed(int groupPosition) {

    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return 0;
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        return 0;
    }
}
