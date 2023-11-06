package hr.ferit.josipmisic.healthcareapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import android.app.Dialog
import android.view.Gravity
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HeartRateMeasurement : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var heartRateSensor: Sensor
    private lateinit var currentHeartRateTextView: TextView
    private lateinit var startButton: Button
    private lateinit var odustaniButton: Button
    private lateinit var spremiButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var heartImageView: ImageView
    private lateinit var lineChart: LineChart
    private lateinit var lastHeartRateProgressBar: ProgressBar
    private lateinit var progressValueTextView: TextView
    private lateinit var maxValueTextView: TextView
    private lateinit var minValueTextView: TextView
    private lateinit var pulseTextView: TextView
    private lateinit var pokusajteTextView: TextView
    private lateinit var vasIzmjereniPulsTextView: TextView
    private lateinit var prosjecanRasponTextView: TextView
    private lateinit var bpmTextView: TextView
    private var isMeasuringHeartRate = false
    private val heartRateMeasurements = mutableListOf<Float>()
    private val ekgEntries = ArrayList<Entry>()

    private var lastHeartRate: Int = 0

    private val heartRateListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
                val heartRateValue = event.values[0]
                heartRateMeasurements.add(heartRateValue)
                updateHeartRateText(heartRateValue.toInt())
                updateProgress(heartRateMeasurements.size)
                if (heartRateMeasurements.size >= 6) {
                    stopHeartRateMeasurement()
                }

                val entry = Entry(heartRateMeasurements.size.toFloat(), heartRateValue)
                ekgEntries.add(entry)
                updateEKGChart()

                lastHeartRate = heartRateValue.toInt()
                updateLastHeartRateText()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Ne koristimo ovu metodu
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate_measurement)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        currentHeartRateTextView = findViewById(R.id.currentHeartRateTextView)
        startButton = findViewById(R.id.startButton)
        odustaniButton = findViewById(R.id.odustaniButton)
        spremiButton = findViewById(R.id.spremiButton)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        progressValueTextView = findViewById(R.id.progressValueTextView)
        maxValueTextView = findViewById(R.id.maxValueTextView)
        minValueTextView = findViewById(R.id.minValueTextView)
        pulseTextView = findViewById(R.id.pulseTextView)
        pokusajteTextView = findViewById(R.id.pokusajteTextView)
        vasIzmjereniPulsTextView = findViewById(R.id.vasIzmjereniPulsTextView)
        prosjecanRasponTextView = findViewById(R.id.prosjecanRasponTextView)
        heartImageView = findViewById<ImageView>(R.id.heartImageView)
        bpmTextView = findViewById(R.id.bpmTextView)
        setupChart()
        lineChart = findViewById(R.id.lineChart)
        lineChart.setDrawGridBackground(false)
        lineChart.axisLeft.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.description.isEnabled = false

        val circularProgressDrawable = CircularProgressDrawable(this).apply {
            strokeWidth = 10f // Debljina linije napretka
            centerRadius = 50f // Radijus kružnog progresbara
            //pokretanje animacije kružnog progress bara
            start()
        }
        //Prikazivanje definiranog animiranog napretka kružnog vrtloga.
        progressBar.indeterminateDrawable = circularProgressDrawable

        val dialogFragment = MyDialogFragment()
        dialogFragment.show(supportFragmentManager, "MyDialogFragment")

        // Podešavanje grafa EKG-a
        val description = Description()
        description.text = ""
        lineChart.description = description
        lineChart.setTouchEnabled(false)
        lineChart.isDragEnabled = false
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)

        startButton.setOnClickListener {
            if (isMeasuringHeartRate) {
                stopHeartRateMeasurement()
            } else {
                startHeartRateMeasurement()
            }
        }

        spremiButton.setOnClickListener{
            updateDatabase()
            finish()
        }

        odustaniButton.setOnClickListener{
            finish()
        }

        lastHeartRateProgressBar = findViewById(R.id.lastHeartRateProgressBar)
        lastHeartRateProgressBar.max = 120
        lastHeartRateProgressBar.progress = 0
    }

    private fun startHeartRateMeasurement() {
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor == null) {
            Toast.makeText(this, "Nedostupan senzor pulsa na uređaju", Toast.LENGTH_SHORT).show()
        } else {
            //Provjera je li dozvola za pristup senzorima tijela već dodijeljena aplikaciji
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
                // Koristenje metode requestPermissions() da bi zatražili dozvolu od korisnika. Metoda requestPermissions() prikazuje dijalog korisniku s zahtjevom za dozvolu pristupa senzorima tijela.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1)
            } else {
                isMeasuringHeartRate = true
                heartRateMeasurements.clear()
                ekgEntries.clear()
                //Registriranje slušača (listener) za senzor otkucaja srca kako bi primao ažuriranja podataka s tog senzora.
                sensorManager.registerListener(heartRateListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
                progressBar.progress = 0
                progressText.text = "0%"
                startButton.text = "Stop"
                currentHeartRateTextView.text = ""
                startHeartAnimation()
                startTextAnimation()
                lastHeartRate = 0
                updateLastHeartRateText()
                lastHeartRateProgressBar.visibility = View.INVISIBLE
                progressValueTextView.visibility = View.INVISIBLE
                maxValueTextView.visibility = View.INVISIBLE
                minValueTextView.visibility = View.INVISIBLE
                vasIzmjereniPulsTextView.visibility = View.INVISIBLE
                prosjecanRasponTextView.visibility = View.INVISIBLE
                odustaniButton.visibility = View.INVISIBLE
                spremiButton.visibility = View.INVISIBLE
                bpmTextView.visibility = View.INVISIBLE
                lineChart.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                pokusajteTextView.visibility = View.VISIBLE
                currentHeartRateTextView.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
            }
        }
    }

    private fun stopHeartRateMeasurement() {
        isMeasuringHeartRate = false
        //Više ne želimo primati ažuriranja sa senzora
        sensorManager.unregisterListener(heartRateListener)
        progressBar.progress = 100
        progressText.text = "100%"
        startButton.text = "Start"
        currentHeartRateTextView.text = ""
        stopHeartAnimation()
        stopTextAnimation()
        lastHeartRateProgressBar.visibility = View.VISIBLE
        lastHeartRateProgressBar.progress = lastHeartRate
        progressValueTextView.visibility = View.VISIBLE
        progressValueTextView.text = getString(R.string.horizontal_progress_bar_result, lastHeartRate)
        maxValueTextView.visibility = View.VISIBLE
        minValueTextView.visibility = View.VISIBLE
        vasIzmjereniPulsTextView.visibility = View.VISIBLE
        prosjecanRasponTextView.visibility = View.VISIBLE
        odustaniButton.visibility = View.VISIBLE
        spremiButton.visibility = View.VISIBLE
        bpmTextView.visibility = View.VISIBLE
        lineChart.visibility = View.INVISIBLE
        progressBar.visibility = View.INVISIBLE
        pokusajteTextView.visibility = View.INVISIBLE
        currentHeartRateTextView.visibility = View.INVISIBLE
        progressText.visibility = View.INVISIBLE
    }

    override fun onPause() {
        super.onPause()
        if (isMeasuringHeartRate) {
            stopHeartRateMeasurement()
        }
    }

    private fun updateHeartRateText(heartRate: Int) {
        currentHeartRateTextView.text = getString(R.string.current_heart_rate_result, heartRate)
    }

    private fun updateProgress(measurementCount: Int) {
        val progress = (measurementCount - 1) * 20
        progressBar.progress = progress
        progressText.text = "$progress%"
    }

    private fun startHeartAnimation() {
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        heartImageView.visibility = View.VISIBLE
        heartImageView.startAnimation(pulseAnimation)
    }

    private fun startTextAnimation() {
        val pulseTextAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_text)
        pulseTextView.visibility = View.VISIBLE
        pulseTextView.startAnimation(pulseTextAnimation)
    }

    private fun stopHeartAnimation() {
        heartImageView.clearAnimation()
        heartImageView.visibility = View.GONE
    }

    private fun stopTextAnimation() {
        pulseTextView.clearAnimation()
        pulseTextView.visibility = View.GONE
    }

    private fun updateEKGChart() {
        val dataSet = LineDataSet(ekgEntries, "EKG")
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 4f
        dataSet.color = ContextCompat.getColor(this, R.color.colorOrange)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(dataSet)

        val lineData = LineData(dataSets)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    private fun setupChart() {
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        lineChart.setNoDataText("") // Postavljanje praznog teksta
        lineChart.invalidate() // Osiguravanje osvježavanja grafikona
    }

    class MyDialogFragment : DialogFragment() {

        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = Dialog(requireActivity())
            dialog.setContentView(R.layout.dialog_fragment_layout)

            // Postavljanje dialoga da bude proziran
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Omogući da dialog fragment bude na dnu
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.gravity = Gravity.BOTTOM
            dialog.window?.attributes = layoutParams

            // Omogući zatvaranje dialog fragmenta klikom izvan njega
            dialog.setCanceledOnTouchOutside(true)

            return dialog
        }
    }


    override fun onRequestPermissionsResult(
        //Prepoznavanje dozvole ako postoji više zahtjeva za dozvolama u istoj aktivnosti
        requestCode: Int,
        //Niz stringova koji sadrži popis traženih dozvola.
        permissions: Array<out String>,
        //Niz integera koji sadrži rezultate korisnikovih odgovora na zahtjev za dozvolom.
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            //PERMISSION_GRANTED ako je dozvola odobrena ili PERMISSION_DENIED ako je odbijena.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startHeartRateMeasurement()
            } else {
                Toast.makeText(this, "Dozvola za pristup senzoru nije odobrena", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateLastHeartRateText() {
        currentHeartRateTextView.text = getString(R.string.current_heart_rate_result, lastHeartRate)
    }

    private fun updateDatabase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (!currentUserEmail.isNullOrEmpty()) {
            val database = FirebaseDatabase.getInstance().reference
            val korisniciRef = database.child("Korisnici")

            val query = korisniciRef.orderByChild("email").equalTo(currentUserEmail)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                //DataSnapshot predstavlja trenutno stanje podataka na određenom čvoru baze podataka u određenom trenutku
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //snapshot predstavlja jedno dijete (child)
                    for (snapshot in dataSnapshot.children) {

                        val user = mapOf<String, Any>(
                            "heartRate" to progressValueTextView.text
                        )

                        // Ažuriraj polja koristeći updateChildren()
                        snapshot.ref.updateChildren(user)

                        // Prekini petlju nakon pronalaska odgovarajućeg korisnika
                        break
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    this@HeartRateMeasurement.runOnUiThread {
                        Toast.makeText(this@HeartRateMeasurement, "Greška prilikom dohvaćanja podataka", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            Toast.makeText(this, "Korisnik nije ulogiran", Toast.LENGTH_SHORT).show()
        }
    }
}
