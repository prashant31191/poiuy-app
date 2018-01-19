package com.quickblox.poiuy.ui.activities.authorization;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.poiuy.R;
import com.quickblox.poiuy.ui.activities.agreements.UserAgreementActivity;
import com.quickblox.poiuy.ui.views.roundedimageview.RoundedImageView;
import com.quickblox.poiuy.utils.KeyboardUtils;
import com.quickblox.poiuy.utils.ToastUtils;
import com.quickblox.poiuy.utils.ValidationUtils;
import com.quickblox.poiuy.utils.helpers.FlurryAnalyticsHelper;
import com.quickblox.poiuy.utils.helpers.GoogleAnalyticsHelper;
import com.quickblox.poiuy.utils.helpers.MediaPickHelper;
import com.quickblox.poiuy.utils.helpers.ServiceManager;
import com.quickblox.poiuy.utils.MediaUtils;
import com.quickblox.poiuy.utils.listeners.OnMediaPickedListener;
import com.quickblox.q_municate_core.core.command.Command;
import com.quickblox.q_municate_core.models.LoginType;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate_db.managers.DataManager;
import com.quickblox.q_municate_db.models.Attachment;
import com.quickblox.q_municate_db.utils.ErrorUtils;
import com.quickblox.q_municate_user_service.model.QMUser;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.soundcloud.android.crop.Crop;

import java.io.File;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import rx.Subscriber;

public class SignUpActivity extends BaseAuthActivity implements OnMediaPickedListener {

    String TAG = this.getClass().getName().toString();
    private static final String FULL_NAME_BLOCKED_CHARACTERS = "<>;";

    @Bind(R.id.full_name_textinputlayout)
    TextInputLayout fullNameInputLayout;

    @Bind(R.id.full_name_edittext)
    EditText fullNameEditText;

    @Bind(R.id.avatar_imageview)
    RoundedImageView avatarImageView;

    private boolean isNeedUpdateImage;
    private QBUser qbUser;
    private Uri imageUri;
    private MediaPickHelper mediaPickHelper;

    private SignUpSuccessAction signUpSuccessAction;
    private UpdateUserSuccessAction updateUserSuccessAction;

    public static void start(Context context) {
        Intent intent = new Intent(context, SignUpActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getContentResId() {
        return R.layout.activity_signup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFields(savedInstanceState);
        setUpActionBarWithUpButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.done_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startLandingScreen();
                break;
            case R.id.action_done:
                if (checkNetworkAvailableWithError()) {
                    signUp();
                }
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        addActions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeActions();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        startLandingScreen();
    }

    @OnClick(R.id.change_avatar_view)
    void selectAvatar(View view) {
        mediaPickHelper.pickAnMedia(this, MediaUtils.IMAGE_REQUEST_CODE);
    }

    @OnTextChanged(R.id.full_name_edittext)
    void onTextChangedFullName(CharSequence text) {
        fullNameInputLayout.setError(null);
    }

    @OnClick(R.id.user_agreement_textview)
    void openUserAgreement(View view) {
        UserAgreementActivity.start(SignUpActivity.this);
    }

    private void initFields(Bundle bundle) {
        title = getString(R.string.auth_sign_up_title);
        qbUser = new QBUser();
        signUpSuccessAction = new SignUpSuccessAction();
        updateUserSuccessAction = new UpdateUserSuccessAction();
        fullNameEditText.setFilters(new InputFilter[]{ fullNameFilter });
        mediaPickHelper = new MediaPickHelper();
    }

    private void startCropActivity(Uri originalUri) {
        imageUri = MediaUtils.getValidUri(new File(getCacheDir(), Crop.class.getName()), this);
        Crop.of(originalUri, imageUri).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            isNeedUpdateImage = true;
            avatarImageView.setImageURI(imageUri);
        } else if (resultCode == Crop.RESULT_ERROR) {
            ToastUtils.longToast(Crop.getError(result).getMessage());
        }
    }

    private void addActions() {
        addAction(QBServiceConsts.SIGNUP_SUCCESS_ACTION, signUpSuccessAction);
        addAction(QBServiceConsts.UPDATE_USER_SUCCESS_ACTION, updateUserSuccessAction);

        updateBroadcastActionList();
    }

    private void removeActions() {
        removeAction(QBServiceConsts.SIGNUP_SUCCESS_ACTION);
        removeAction(QBServiceConsts.UPDATE_USER_SUCCESS_ACTION);

        updateBroadcastActionList();
    }

    private void signUp() {
        KeyboardUtils.hideKeyboard(this);

        loginType = LoginType.EMAIL;

        String fullNameText = fullNameEditText.getText().toString();
        String emailText = emailEditText.getText().toString();
        String passwordText = passwordEditText.getText().toString();

        if (new ValidationUtils(this).isSignUpDataValid(fullNameInputLayout, emailTextInputLayout,
                passwordTextInputLayout, fullNameText, emailText, passwordText)) {
                qbUser.setFullName(fullNameText);
                qbUser.setEmail(emailText);
                qbUser.setPassword(passwordText);

            showProgress();

            if (isNeedUpdateImage && imageUri != null) {
                startSignUp(MediaUtils.getCreatedFileFromUri(imageUri));
            } else {
                appSharedHelper.saveUsersImportInitialized(false);
                startSignUp(null);
            }
        }
    }

    private void startSignUp(final File imageFile) {
        DataManager.getInstance().clearAllTables();
       // QBSignUpCommand.start(SignUpActivity.this, qbUser, imageFile);

        QBUsers.signUpSignInTask(qbUser).performAsync(new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {

                if(qbUser.getLogin() !=null) {
                    Log.e(TAG, "signUpFromMainActivity onSuccess" + qbUser.getLogin());
                }
                if(bundle ==null)
                {
                    bundle = new Bundle();
                }
                //startMainActivity(qbUser);
                if(qbUser !=null) {
                    bundle.putSerializable(QBServiceConsts.EXTRA_USER, qbUser);
                }
                if(imageFile !=null) {
                    bundle.putSerializable(QBServiceConsts.EXTRA_FILE, imageFile);
                    performSignUpSuccessAction(bundle);
                }
               else {
                    performUpdateUserSuccessAction(bundle);
                }
            }

            @Override
            public void onError(QBResponseException error) {
                Log.e(TAG, "signUpFromMainActivity onError " + error.getMessage());
            }
        });
    }

    protected void performUpdateUserSuccessAction(Bundle bundle) {
        QBUser user = (QBUser) bundle.getSerializable(QBServiceConsts.EXTRA_USER);
        appSharedHelper.saveFirstAuth(true);
        appSharedHelper.saveSavedRememberMe(true);
        startMainActivity(user);

        // send analytics data
        GoogleAnalyticsHelper.pushAnalyticsData(SignUpActivity.this, user, "User Sign Up");
        //send flurry
        FlurryAnalyticsHelper.passingUserData(user);
    }

    private void performSignUpSuccessAction(Bundle bundle) {
        File image = null;
        QBUser user = null;
        if(bundle.getSerializable(QBServiceConsts.EXTRA_FILE) !=null) {
            image = (File) bundle.getSerializable(QBServiceConsts.EXTRA_FILE);
        }
        if(bundle.getSerializable(QBServiceConsts.EXTRA_USER) !=null) {
            user = (QBUser) bundle.getSerializable(QBServiceConsts.EXTRA_USER);
        }

        ServiceManager.getInstance().updateUser(user, image).subscribe(new Subscriber<QMUser>() {
            @Override
            public void onCompleted() {
                Log.d(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError" + e.getMessage());
            }

            @Override
            public void onNext(QMUser qmUser) {
                Log.d(TAG, "onNext");
                appSharedHelper.saveFirstAuth(true);
                appSharedHelper.saveSavedRememberMe(true);
                startMainActivity(qmUser);

                // send analytics data
                GoogleAnalyticsHelper.pushAnalyticsData(SignUpActivity.this, qmUser, "User Sign Up");

                //send flurry
                FlurryAnalyticsHelper.passingUserData(qmUser);
            }
        });
    }

    private InputFilter fullNameFilter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source != null && FULL_NAME_BLOCKED_CHARACTERS.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };

    @Override
    public void onMediaPicked(int requestCode, Attachment.Type attachmentType, Object attachment) {
        if (Attachment.Type.IMAGE.equals(attachmentType)) {
            startCropActivity(MediaUtils.getValidUri((File)attachment, this));
        }
    }

    @Override
    public void onMediaPickError(int requestCode, Exception e) {
        ErrorUtils.showError(this, e);
    }

    @Override
    public void onMediaPickClosed(int requestCode) {
    }

    private class SignUpSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) throws Exception {
            appSharedHelper.saveUsersImportInitialized(false);
            performSignUpSuccessAction(bundle);
        }
    }

    private class UpdateUserSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) throws Exception {
            performUpdateUserSuccessAction(bundle);
        }
    }
}