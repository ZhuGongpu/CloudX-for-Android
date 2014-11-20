package cloudx.views.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import cloudx.main.R;
import com.squareup.picasso.Picasso;
import model.FileEntity;

import java.util.ArrayList;


/**
 * Created by zhugongpu on 14/11/17.
 */
public class FileListFragment extends Fragment {
    private static String TAG = "FileListFragment";

    private ListView fileList = null;
    private View fragmentView = null;
    private BaseAdapter adapter = null;

    private ArrayList<FileEntity> data = new ArrayList<FileEntity>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (fragmentView == null) {
            init(inflater, container);
            loadData();
        }

        ViewGroup parent = (ViewGroup) fragmentView.getParent();
        if (parent != null)
            parent.removeView(fragmentView);


        return fragmentView;
    }

    private void init(LayoutInflater inflater, ViewGroup container) {
        fragmentView = inflater.inflate(R.layout.file_list_fragment, container, false);
        fileList = (ListView) fragmentView.findViewById(R.id.file_list);

        this.adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public Object getItem(int position) {
                return data.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                FileEntity dataItem = data.get(position);

                ViewHolder holder;
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = View.inflate(getActivity(), R.layout.list_item, null);

                    holder.fileIcon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.fileName = (TextView) convertView.findViewById(R.id.title);
                    holder.fileSize = (TextView) convertView.findViewById(R.id.body);

                    convertView.findViewById(R.id.action).setVisibility(View.GONE);

                    convertView.setTag(holder);
                } else
                    holder = (ViewHolder) convertView.getTag();

                //TODO
                Picasso.with(getActivity()).load(dataItem.getFileIcon()).into(holder.fileIcon);
                holder.fileName.setText(dataItem.fileName);
                holder.fileSize.setText(dataItem.fileSize);

                return convertView;
            }

            class ViewHolder {
                public ImageView fileIcon = null;
                public TextView fileName = null;
                public TextView fileSize = null;
            }
        };
        this.fileList.setAdapter(this.adapter);
        this.fileList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FileListFragment.this.onItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        registerForContextMenu(fileList);
    }

    private void onItemSelected(int position) {
        //TODO undefined
    }

    private void loadData() {

        for (int i = 0; i < 10; i++) {
            FileEntity file = new FileEntity();
            file.fileType = FileEntity.FileType.Others;
            file.fileName = "fileName";
            file.fileSize = "20KB";

            this.data.add(file);
        }
        this.adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(getText(R.string.Delete));
    }




}