package com.alexcrist.redditreadonly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.alexcrist.redditreadonly.MyApplication;
import com.alexcrist.redditreadonly.PostExecute;
import com.alexcrist.redditreadonly.R;
import com.alexcrist.redditreadonly.adapter.SubmissionAdapter;
import com.alexcrist.redditreadonly.loader.LoadPage;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.gc.materialdesign.views.ProgressBarIndeterminate;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;

public class BrowseActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
  ListView.OnScrollListener, SwipeMenuListView.OnMenuItemClickListener, PostExecute {

  private SubredditPaginator paginator;
  private SubmissionAdapter adapter;
  private ProgressBarIndeterminate progressBar;
  private boolean loading;

  // Initialization
  // -----------------------------------------------------------------------------------------------

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_browse);

    progressBar = (ProgressBarIndeterminate) findViewById(R.id.progressBar);
    RedditClient redditClient = ((MyApplication) this.getApplication()).getRedditClient();
    paginator = new SubredditPaginator(redditClient);
    adapter = new SubmissionAdapter(this, R.layout.submission_layout, new ArrayList<Submission>());
    initListView();
    loading = false;
    loadPage();
  }

  private void initListView() {
    SwipeMenuCreator creator = new SwipeMenuCreator() {
      @Override
      public void create(SwipeMenu menu) {
        SwipeMenuItem comment = new SwipeMenuItem(getApplicationContext());
        comment.setBackground(R.drawable.comment_bg);
        comment.setWidth(310);
        menu.addMenuItem(comment);
      }
    };

    SwipeMenuListView listView = (SwipeMenuListView) findViewById(R.id.submissionListView);
    listView.setMenuCreator(creator);
    listView.setOnItemClickListener(this);
    listView.setOnMenuItemClickListener(this);
    listView.setOnScrollListener(this);
    listView.setAdapter(adapter);
  }

  // Load a page of submissions
  // -----------------------------------------------------------------------------------------------

  private void loadPage() {
    if (((MyApplication) this.getApplication()).getRedditClient().isAuthenticated()) {
      if (!loading) {
        loading = true;
        progressBar.setVisibility(ProgressBar.VISIBLE);
        new LoadPage(paginator, adapter, this).execute();
      }
    } else {
      ((MyApplication) this.getApplication()).reauthenticate(new PostExecute() {
        @Override
        public void onPostExecute() {
          loadPage();
        }
      });
    }
  }

  @Override
  public void onPostExecute() {
    loading = false;
    progressBar.setVisibility(ProgressBar.GONE);
  }

  // On user click
  // -----------------------------------------------------------------------------------------------

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
    Submission submission = adapter.getItem(index);
    if (submission.isSelfPost()) {
      gotoComments(index);
    } else {
      Intent intent = new Intent(this, ViewingActivity.class);
      intent.putExtra("url", submission.getUrl());
      startActivity(intent);
    }
  }

  @Override
  public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
    switch (index) {
      // go to comments
      case 0:
        gotoComments(position);
        return true;
    }
    return false;
  }

  // On user scroll
  // -----------------------------------------------------------------------------------------------

  @Override
  public void onScrollStateChanged(AbsListView absListView, int i) { }

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                       int totalItemCount) {
    switch (view.getId()) {
      // load the next page when you're 15 items away from the bottom
      case R.id.submissionListView:
        final int lastItem = firstVisibleItem + visibleItemCount;
        if (lastItem >= totalItemCount - 15) {
          loadPage();
        }
    }
  }

  // On user presses back button
  // -----------------------------------------------------------------------------------------------

  @Override
  public void onBackPressed() { }

  // Go to comments
  // -----------------------------------------------------------------------------------------------

  private void gotoComments(final int index) {
    if (((MyApplication) this.getApplication()).getRedditClient().isAuthenticated()) {
      Intent intent = new Intent(this, CommentActivity.class);
      intent.putExtra("submissionId", adapter.getItem(index).getId());
      startActivity(intent);
    } else {
      ((MyApplication) this.getApplication()).reauthenticate(new PostExecute() {
        @Override
        public void onPostExecute() {
          gotoComments(index);
        }
      });
    }
  }
}