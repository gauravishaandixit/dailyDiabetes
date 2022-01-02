package com.dailydiabetes.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dailydiabetes.R
import com.dailydiabetes.adapter.ShowAllAdapter
import com.dailydiabetes.constant.Constants.Companion.TOPIC_NAME
import com.dailydiabetes.model.DiabetesValue
import com.dailydiabetes.model.NotificationData
import com.dailydiabetes.model.PushNotification
import com.dailydiabetes.retrofit.RetrofitInstance
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), ShowAllAdapter.DiabetesValueClickListener{

    private val TAG = "MainActivity"

    private lateinit var addNew: FloatingActionButton

    private lateinit var valueEditText: EditText
    private lateinit var timeChipGroup: ChipGroup
    private lateinit var saveValue: Button
    private lateinit var datePicker: Button
    private lateinit var cancel: Button

    private lateinit var database: DatabaseReference

    var isDateChanged = false

    private lateinit var view: View
    private lateinit var alertDialog: AlertDialog

    lateinit var recyclerView: RecyclerView
    lateinit var diabetesValueList: ArrayList<DiabetesValue>

    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG, "onCreate: ")

        addNew = findViewById(R.id.addNew)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        diabetesValueList = arrayListOf()

        database = Firebase.database.getReference("diabetesValues")

        view = initializeNewIssueLayout()

        addNew.setOnClickListener {
            val alertDialogBuilder = MaterialAlertDialogBuilder(this)

            if (view.parent != null)
                (view.parent as ViewGroup).removeView(view)

            alertDialogBuilder.setView(view)
            alertDialogBuilder.setCancelable(false)

            alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                diabetesValueList = arrayListOf()
                if(snapshot.exists()) {
                    for (diabetesValues in snapshot.children) {
                        val diabetesValue = diabetesValues.getValue(DiabetesValue::class.java)

                        if (diabetesValue != null) {
                            diabetesValueList.add(diabetesValue)
                        }
                    }
                    diabetesValueList.sortBy { -it.date }
                    recyclerView.adapter = ShowAllAdapter(diabetesValueList, this@MainActivity)
                    progressBar.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_NAME).addOnCompleteListener {
            Toast.makeText(
                this,
                "Subscribed to $TOPIC_NAME? ${it.isSuccessful}",
                Toast.LENGTH_SHORT).show()
            Log.i(TAG, "onCreate: subscribed? = ${it.isSuccessful}")
        }
    }

    private fun initializeNewIssueLayout(): View {
        val view = layoutInflater.inflate(R.layout.add_new_layout, null, false)
        valueEditText = view.findViewById(R.id.value)
        timeChipGroup = view.findViewById(R.id.chipGroup)
        saveValue = view.findViewById(R.id.saveValue)
        datePicker = view.findViewById(R.id.datePicker)
        cancel = view.findViewById(R.id.cancel)

        datePicker.setOnClickListener {
            openDatePicker()
        }

        var time = ""

        timeChipGroup.setOnCheckedChangeListener(object : ChipGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: ChipGroup?, checkedId: Int) {
                when(checkedId) {
                    R.id.morning -> time = "Morning"
                    R.id.lunch -> time = "Lunch"
                    R.id.night -> time = "Night"
                    View.NO_ID -> time = ""
                }
            }
        })

        saveValue.setOnClickListener {
            saveToFirebase(valueEditText.text.toString(), time)
            valueEditText.text.clear()
            datePicker.text = "Pick Date"
            timeChipGroup.clearCheck()
        }

        cancel.setOnClickListener {
            valueEditText.text.clear()
            datePicker.text = "Pick Date"
            timeChipGroup.clearCheck()
            alertDialog.dismiss()
        }

        return view
    }

    private fun saveToFirebase(value: String, time: String) {
        if(value.isNotEmpty() && time.isNotEmpty()) {
            var date = Calendar.getInstance().time

            // to be added when date is selected from date picker
            var todayTime = date.time % 86400000

            if (isDateChanged) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                date = sdf.parse(datePicker.text.toString())
                    ?: date

                todayTime += (5 * 60 * 60 + 30 * 60) * 1000
            }

            val currentTime = if (isDateChanged) {
                date.time + todayTime
            } else {
                date.time
            }

            val diabetes = DiabetesValue(value.toInt(), time = time, date = currentTime)

            database.child(currentTime.toString()).setValue(diabetes).addOnSuccessListener {
                val text = "$value at $time on ${Date(currentTime).toLocaleString()} Saved!!!"
                Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
                sendMessageToFirebase(text)
                alertDialog.dismiss()
            }
        }
        else {
            Toast.makeText(
                this@MainActivity,
                "Value AND Time is required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openDatePicker() {
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, DatePickerDialog.OnDateSetListener {
                view, year, month, day ->
            isDateChanged = true
            val selectedDate = "${day}/${month + 1}/${year}"
            datePicker.text = selectedDate
        },
            todayYear,
            todayMonth,
            todayDay).show()
    }

    override fun onDiabetesValueClickListener(position: Int) {
        val value = diabetesValueList[position].value
        val time = diabetesValueList[position].time
        val date = diabetesValueList[position].date

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Delete ?")
        alertDialog.setMessage("Are you sure to delete $value at $time on ${Date(date).toLocaleString()} ?")
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { p0, p1 -> p0?.cancel() }
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Delete") { p0, p1 ->
            val databaseReference = Firebase.database
                .getReference("diabetesValues/$date")

            databaseReference.removeValue()
        }
        alertDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.title) {
            "Graphs" -> startActivity(Intent(this, GraphActivity::class.java))
            "Last One Week" -> createReport(7)
            "Last Two Weeks" -> createReport(14)
            "Last One Month" -> createReport(30)
            "Last Two Months" -> createReport(60)
        }
        return super.onOptionsItemSelected(item)
    }
    private fun createReport(days: Int) {
        Log.i(TAG, "createReport: $days")
        try {
            val currentTime = Calendar.getInstance().time.time

            var filePath = getExternalFilesDir(null)?.absolutePath + "/" + Date() + ".csv"
            filePath = filePath.replace(" ", "_")
            filePath = filePath.replace(":", "_")
            filePath = filePath.replace("+", "_")

            Log.i(TAG, "createReport: $filePath")

            val file = File(filePath)

            val bw = BufferedWriter(FileWriter(file, true))

            bw.append("Date,Time Of Day,Value\n")

            diabetesValueList.forEach {
                val diffOfDays = ((currentTime - it.date) / 86400000)

                if(diffOfDays < days) {
                    bw.append(Date(it.date).toString())
                    bw.append(",")
                    bw.append(it.time)
                    bw.append(",")
                    bw.append(it.value.toString())
                    bw.append("\n")
                }
            }
            Snackbar.make(findViewById(android.R.id.content), "Data written to '$filePath'.",
                Snackbar.LENGTH_SHORT).show()
            bw.close()
        } catch (e: Exception) {
            Log.e(TAG, "createReport: $e")
        }
    }

    private fun sendMessageToFirebase(text: String) {
        Log.i(TAG, "sendMessageToFirebase: ")
        val notification = PushNotification(
            NotificationData("New Diabetes Value", text),
            TOPIC_NAME
        )
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if(response.isSuccessful) {
                    Log.d(TAG, "sendMessageToFirebase: Response is $response")
                } else {
                    Log.e(TAG, "sendMessageToFirebase: ${response.errorBody().toString()}")
                }
            } catch(e: Exception) {
                Log.e(TAG, "sendMessageToFirebase: $e")
            }
        }
    }
}
