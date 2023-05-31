/**
 * ownCloud Android client application
 *
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.extensions.ActivityExtKt;
import com.owncloud.android.extensions.FragmentExtKt;
import com.owncloud.android.extensions.MenuExtKt;
import com.owncloud.android.presentation.files.operations.FileOperation;
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel;
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment;
import com.owncloud.android.presentation.previews.PreviewTextViewModel;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.PreferenceUtils;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import timber.log.Timber;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import static org.koin.java.KoinJavaComponent.get;

public class PreviewTextFragment extends FileFragment {
    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT";

    private Account mAccount;
    private ProgressBar mProgressBar;
    private TransferProgressController mProgressController;
    private TextView mTextPreview;
    private TextLoadAsyncTask mTextLoadTask;

    PreviewTextViewModel previewTextViewModel = get(PreviewTextViewModel.class);
    FileOperationsViewModel fileOperationsViewModel = get(FileOperationsViewModel.class);

    /**
     * Public factory method to create new PreviewTextFragment instances.
     *
     * @param file    An {@link OCFile} to preview in the fragment
     * @param account ownCloud account containing file
     * @return Fragment ready to be used.
     */
    public static PreviewTextFragment newInstance(
            OCFile file,
            Account account
    ) {
        PreviewTextFragment frag = new PreviewTextFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_FILE, file);
        args.putParcelable(EXTRA_ACCOUNT, account);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates an empty fragment for previews.
     * <p/>
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     * <p/>
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewTextFragment() {
        super();
        mAccount = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Timber.v("onCreateView");

        View ret = inflater.inflate(R.layout.preview_text_fragment, container, false);
        ret.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        mProgressBar = ret.findViewById(R.id.syncProgressBar);
        mTextPreview = ret.findViewById(R.id.text_preview);

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OCFile file;
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            file = args.getParcelable(EXTRA_FILE);
            mAccount = args.getParcelable(EXTRA_ACCOUNT);
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (mAccount == null) {
                throw new IllegalStateException("Instanced with a NULL ownCloud Account");
            }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(EXTRA_ACCOUNT);
        }
        setFile(file);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);
        mProgressController = new TransferProgressController(mContainerActivity);
        mProgressController.setProgressBar(mProgressBar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_FILE, getFile());
        outState.putParcelable(EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.v("onStart");

        loadAndShowTextPreview();
    }

    private void loadAndShowTextPreview() {
        mTextLoadTask = new TextLoadAsyncTask(new WeakReference<>(mTextPreview));
        mTextLoadTask.execute(getFile());
    }

    /**
     * Reads the file to preview and shows its contents. Too critical to be anonymous.
     */
    private class TextLoadAsyncTask extends AsyncTask<Object, Void, StringWriter> {
        private final String DIALOG_WAIT_TAG = "DIALOG_WAIT";
        private final WeakReference<TextView> mTextViewReference;
        private String mimeType;

        private TextLoadAsyncTask(WeakReference<TextView> textView) {
            mTextViewReference = textView;
        }

        @Override
        protected void onPreExecute() {
            showLoadingDialog();
        }

        @Override
        protected StringWriter doInBackground(java.lang.Object... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("The parameter to " + TextLoadAsyncTask.class.getName() + " must " +
                        "be (1) the file location");
            }
            final OCFile file = (OCFile) params[0];
            final String location = file.getStoragePath();
            mimeType = file.getMimeType();

            FileInputStream inputStream = null;
            Scanner sc = null;
            StringWriter source = new StringWriter();
            BufferedWriter bufferedWriter = new BufferedWriter(source);
            try {
                inputStream = new FileInputStream(location);
                sc = new Scanner(inputStream);
                while (sc.hasNextLine()) {
                    bufferedWriter.append(sc.nextLine());
                    if (sc.hasNextLine()) {
                        bufferedWriter.append("\n");
                    }
                }
                bufferedWriter.close();
                IOException exc = sc.ioException();
                if (exc != null) {
                    throw exc;
                }
            } catch (IOException e) {
                Timber.e(e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                }
                if (sc != null) {
                    sc.close();
                }
            }
            return source;
        }

        @Override
        protected void onPostExecute(final StringWriter stringWriter) {
            final TextView textView = mTextViewReference.get();

            if (textView != null) {
                String text = new String(stringWriter.getBuffer());
                if (mimeType.equals("text/markdown")) {
                    Context context = textView.getContext();
                    Markwon markwon = Markwon
                            .builder(context)
                            .usePlugin(TablePlugin.create(context))
                            .usePlugin(StrikethroughPlugin.create())
                            .usePlugin(TaskListPlugin.create(context))
                            .usePlugin(HtmlPlugin.create())
                            .build();
                    markwon.setMarkdown(textView, text);
                } else {
                    textView.setText(text);
                }
                textView.setVisibility(View.VISIBLE);
            }

            try {
                dismissLoadingDialog();
            } catch (IllegalStateException illegalStateException) {
                Timber.w(illegalStateException, "Dismissing dialog not allowed after onSaveInstanceState");
            }
        }

        /**
         * Show loading dialog
         */
        public void showLoadingDialog() {
            // only once
            Fragment frag = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
            LoadingDialog loading = null;
            if (frag == null) {
                // Construct dialog
                loading = LoadingDialog.newInstance(R.string.wait_a_moment, false);
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                loading.show(ft, DIALOG_WAIT_TAG);
            } else {
                loading = (LoadingDialog) frag;
                loading.setShowsDialog(true);
            }

        }

        /**
         * Dismiss loading dialog
         */
        public void dismissLoadingDialog() {
            final Fragment frag = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
            if (frag != null) {
                LoadingDialog loading = (LoadingDialog) frag;
                loading.dismiss();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mContainerActivity.getStorageManager() != null) {
            OCFile safeFile = getFile();
            String accountName = mContainerActivity.getStorageManager().getAccount().name;
            previewTextViewModel.filterMenuOptions(safeFile, accountName);

            FragmentExtKt.collectLatestLifecycleFlow(this, previewTextViewModel.getMenuOptions(), Lifecycle.State.CREATED,
                    (menuOptions, continuation) -> {
                        boolean hasWritePermission = safeFile.getHasWritePermission();
                        MenuExtKt.filterMenuOptions(menu, menuOptions, hasWritePermission);
                        return null;
                    }
            );
        }

        MenuItem item = menu.findItem(R.id.action_search);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstanceForSingleFile(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                ActivityExtKt.sendDownloadedFilesByShareSheet(requireActivity(), Collections.singletonList(getFile()));
                return true;
            }
            case R.id.action_sync_file: {
                fileOperationsViewModel.performOperation(new FileOperation.SynchronizeFileOperation(getFile(), mAccount.name));
                return true;
            }
            case R.id.action_set_available_offline: {
                ArrayList<OCFile> fileToSetAsAvailableOffline = new ArrayList<>();
                fileToSetAsAvailableOffline.add(getFile());
                fileOperationsViewModel.performOperation(new FileOperation.SetFilesAsAvailableOffline(fileToSetAsAvailableOffline));
                return true;
            }
            case R.id.action_unset_available_offline: {
                ArrayList<OCFile> fileToUnsetAsAvailableOffline = new ArrayList<>();
                fileToUnsetAsAvailableOffline.add(getFile());
                fileOperationsViewModel.performOperation(new FileOperation.UnsetFilesAsAvailableOffline(fileToUnsetAsAvailableOffline));
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void seeDetails() {
        mContainerActivity.showDetails(getFile());
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.v("onStop");
        if (mTextLoadTask != null) {
            mTextLoadTask.cancel(Boolean.TRUE);
            mTextLoadTask.dismissLoadingDialog();
        }
    }

    @Override
    public void onFileMetadataChanged(OCFile updatedFile) {
        if (updatedFile != null) {
            setFile(updatedFile);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onFileMetadataChanged() {
        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {
            setFile(storageManager.getFileByPath(getFile().getRemotePath(), null));
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onFileContentChanged() {
        loadAndShowTextPreview();
    }

    @Override
    public void updateViewForSyncInProgress() {
        mProgressController.showProgressBar();
    }

    @Override
    public void updateViewForSyncOff() {
        mProgressController.hideProgressBar();
    }

    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewTextFragment} to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        final List<String> unsupportedTypes = new LinkedList<String>();
        unsupportedTypes.add("text/richtext");
        unsupportedTypes.add("text/rtf");
        unsupportedTypes.add("text/vnd.abc");
        unsupportedTypes.add("text/vnd.fmi.flexstor");
        unsupportedTypes.add("text/vnd.rn-realtext");
        unsupportedTypes.add("text/vnd.wap.wml");
        unsupportedTypes.add("text/vnd.wap.wmlscript");
        return (file != null && file.isAvailableLocally() && file.isText() &&
                !unsupportedTypes.contains(file.getMimeType()) &&
                !unsupportedTypes.contains(file.getMimeTypeFromName())
        );
    }

    /**
     * Finishes the preview
     */
    private void finish() {
        getActivity().onBackPressed();
    }
}
