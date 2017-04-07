package edu.uark.uarkregisterapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.uark.uarkregisterapp.models.api.ActiveEmployeeCounts;
import edu.uark.uarkregisterapp.models.api.Employee;
import edu.uark.uarkregisterapp.models.api.EmployeeLogin;
import edu.uark.uarkregisterapp.models.api.enums.EmployeeApiRequestStatus;
import edu.uark.uarkregisterapp.models.api.services.EmployeeService;
import edu.uark.uarkregisterapp.models.transition.EmployeeTransition;

public class LandingActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landing);
	}

	@Override
	protected void onStart() {
		super.onStart();

		(new QueryActiveEmployeeCountsTask()).execute();
	}

	public void signInButtonOnClick(View view) {
		if (StringUtils.isBlank(this.getEmployeeIdEditText().getText().toString())) {
			new AlertDialog.Builder(this).
					setMessage(R.string.alert_dialog_employee_id_empty).
					create().
					show();
			this.getEmployeeIdEditText().requestFocus();

			return;
		}

		if (StringUtils.isBlank(this.getPasswordEditText().getText().toString())) {
			new AlertDialog.Builder(this).
					setMessage(R.string.alert_dialog_employee_password_empty).
					create().
					show();
			this.getPasswordEditText().requestFocus();

			return;
		}

		//TODO: I need to hash the password.
		(new SignInTask()).execute(
				(new EmployeeLogin()).
						setEmployeeId(this.getEmployeeIdEditText().getText().toString()).
						setPassword(this.getPasswordEditText().getText().toString())
		);
	}

	private EditText getEmployeeIdEditText() {
		return (EditText) this.findViewById(R.id.edit_text_employee_id);
	}

	private EditText getPasswordEditText() {
		return (EditText) this.findViewById(R.id.edit_text_password);
	}

	private class QueryActiveEmployeeCountsTask extends AsyncTask<Void, Void, ActiveEmployeeCounts> {
		@Override
		protected ActiveEmployeeCounts doInBackground(Void... params) {
			return (new EmployeeService()).activeEmployeeCounts();
		}

		@Override
		protected void onPostExecute(ActiveEmployeeCounts param) {
			if (this.activeEmployeeExists(param)) {
				return;
			}

			new AlertDialog.Builder(LandingActivity.this).
					setMessage(R.string.alert_dialog_no_employees_exist).
					setPositiveButton(
							R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									startActivity(new Intent(getApplicationContext(), CreateEmployeeActivity.class));

									dialog.dismiss();
								}
							}
					).
					create().
					show();
		}

		private boolean activeEmployeeExists(ActiveEmployeeCounts activeEmployeeCounts) {
			return (
					(activeEmployeeCounts.getActiveCashierCount() > 0) ||
							(activeEmployeeCounts.getActiveShiftManagerCount() > 0) ||
							(activeEmployeeCounts.getActiveGeneralManagerCount() > 0)
			);
		}
	}

	private class SignInTask extends AsyncTask<EmployeeLogin, Void, Employee> {
		@Override
		protected void onPreExecute() {
			this.signInAlert = new AlertDialog.Builder(LandingActivity.this).
					setMessage(R.string.alert_dialog_signing_in).
					create();
			this.signInAlert.show();
		}

		@Override
		protected Employee doInBackground(EmployeeLogin... employeeLogins) {
			if (employeeLogins.length > 0) {
				try {
					MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
					messageDigest.update(employeeLogins[0].getPassword().getBytes());
					employeeLogins[0].setPassword(new String(messageDigest.digest()));
				} catch (NoSuchAlgorithmException e) { }

				return (new EmployeeService()).logIn(employeeLogins[0]);
			} else {
				return (new Employee()).
						setApiRequestStatus(EmployeeApiRequestStatus.NOT_FOUND);
			}
		}

		@Override
		protected void onPostExecute(Employee employee) {
			this.signInAlert.dismiss();

			if (employee.getApiRequestStatus() != EmployeeApiRequestStatus.OK) {
				new AlertDialog.Builder(LandingActivity.this).
						setMessage(R.string.alert_dialog_employee_sign_in_failed).
						create().
						show();
				return;
			}

			Intent intent = new Intent(getApplicationContext(), MainActivity.class);

			intent.putExtra(
					getString(R.string.intent_extra_employee),
					new EmployeeTransition(employee)
			);

			startActivity(intent);
		}

		private AlertDialog signInAlert;
	}
}
