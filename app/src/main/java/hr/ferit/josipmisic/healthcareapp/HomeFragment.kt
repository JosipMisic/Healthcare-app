package hr.ferit.josipmisic.healthcareapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), MealDialogFragment.MealDialogListener {

    private var glassCount = 0
    private var totalCalories = 0
    private var savedScrollPosition: Int = 0
    private val timerInterval: Long = 100 // 100 milisekundi
    private var timerJob: Job? = null
    private lateinit var scrollView: ScrollView
    private lateinit var caseTextView: TextView
    private lateinit var caseCountTextView: TextView
    private lateinit var izmjeriButton: Button
    private lateinit var decrementButton: Button
    private lateinit var incrementButton: Button
    private lateinit var glassImageView: ImageView
    private lateinit var lemonImageView: ImageView
    private lateinit var unesiObrokButton: Button
    private val mealsList: MutableList<Meal> = mutableListOf()
    private lateinit var mealContainer: LinearLayout
    private lateinit var numberPicker: NumberPicker
    private lateinit var kgTextView: TextView
    private lateinit var bpmTextView: TextView

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflater koristi XML layout za prikazivanje fragmenta
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView = view.findViewById(R.id.scrollView)

        //Praćenje scrollView-a i spremanje u globalnu varijablu savedScrollPosition koja se koristi u onResume metodi
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            savedScrollPosition = scrollView.scrollY
        }

        izmjeriButton = view.findViewById(R.id.izmjeriButton)
        caseTextView = view.findViewById(R.id.caseTextView)
        caseCountTextView = view.findViewById(R.id.caseCountTextView)
        decrementButton = view.findViewById<Button>(R.id.decrementButton)
        incrementButton = view.findViewById<Button>(R.id.incrementButton)
        glassImageView = view.findViewById(R.id.glassImageView)
        lemonImageView = view.findViewById(R.id.lemonImageView)
        unesiObrokButton = view.findViewById(R.id.unesiObrokButton)
        mealContainer = view.findViewById(R.id.mealContainer)
        kgTextView = view.findViewById(R.id.kgTextView)
        bpmTextView = view.findViewById(R.id.bpmTextView)
        numberPicker = view.findViewById<NumberPicker>(R.id.numberPicker)
        animateLemon()

        numberPicker.textColor = Color.WHITE
        numberPicker.minValue = 2 // Minimalna vrijednost
        numberPicker.maxValue = 500 // Maksimalna vrijednost
        numberPicker.value = 78 // Trenutna vrijednost
        numberPicker.wrapSelectorWheel = true // Omogući rotiranje vrijednosti


        unesiObrokButton.setOnClickListener {
            val dialogFragment = MealDialogFragment()
            dialogFragment.setMealDialogListener(this@HomeFragment)
            dialogFragment.show(childFragmentManager, "MealDialogFragment")
        }


        decrementButton.isEnabled = false


        izmjeriButton.setOnClickListener {
            val intent = Intent(activity, HeartRateMeasurement::class.java)
            startActivity(intent)
        }

        decrementButton.setOnClickListener {
            decreaseGlassCount()
        }

        incrementButton.setOnClickListener {
            increaseGlassCount()
        }

        loadUserData()
        updateGlassCount()
        startTimer()
    }


    private fun decreaseGlassCount() {
        if (glassCount > 0) {
            glassCount--
            updateGlassCount()
        }
        decrementButton.isEnabled = glassCount != 0
    }

    private fun increaseGlassCount() {
        glassCount++
        updateGlassCount()
        decrementButton.isEnabled = glassCount != 0
    }

    override fun onResume() {
        super.onResume()
        decrementButton.isEnabled = glassCount != 0

        // Spremanje trenutnog položaja ScrollView-a
        scrollView.scrollTo(0, savedScrollPosition)
    }

    private fun updateGlassCount() {
        caseCountTextView.text = glassCount.toString()

        if (glassCount == 2 || glassCount == 3 || glassCount == 4) {
            caseTextView.text = "čaše"
        } else {
            caseTextView.text = "čaša"
        }
        animateGlass()
        updateMillilitersTextView()
    }

    private fun animateGlass() {
        val targetDrawable = when (glassCount) {
            0 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_0)
            1 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_1)
            2 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_2)
            3 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_3)
            4 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_4)
            5 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_5)
            6 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_6)
            7 -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_7)
            else -> ContextCompat.getDrawable(requireContext(), R.drawable.glass_8)
        }

        val transition = glassImageView.drawable as? TransitionDrawable
        transition?.startTransition(500)
        glassImageView.setImageDrawable(targetDrawable)
    }

    private fun animateLemon() {
        val targetDrawable = when (totalCalories) {
            0 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_0)
            in 1..222 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_1)
            in 223..445 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_2)
            in 446..668 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_3)
            in 669..891 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_4)
            in 892..1114 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_5)
            in 1115..1337 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_6)
            in 1338..1560 -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_7)
            else -> ContextCompat.getDrawable(requireContext(), R.drawable.lemon_8)
        }

        val transition = lemonImageView.drawable as? TransitionDrawable
        transition?.startTransition(500)
        lemonImageView.setImageDrawable(targetDrawable)
    }

    private fun calculateMilliliters(): Int {
        val millilitersPerGlass = 250
        return glassCount * millilitersPerGlass
    }

    private fun updateMillilitersTextView() {
        val milliliters = calculateMilliliters()
        val millilitersTextView: TextView = view?.findViewById(R.id.millilitersTextView) ?: return
        millilitersTextView.text = milliliters.toString()
    }

    override fun onMealEntered(selectedOption: String, calories: Int) {
        val meal = Meal(selectedOption, calories)
        mealsList.add(meal)
        totalCalories += calories

        val mealTextView = layoutInflater.inflate(R.layout.meal_text_view, null) as TextView
        mealTextView.text = "${meal.selectedOption}: ${meal.calories} kcal"
        mealContainer.addView(mealTextView)

        val mealTextViewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        mealTextViewParams.gravity = Gravity.CENTER_HORIZONTAL
        mealTextView.layoutParams = mealTextViewParams

        val deleteButton = layoutInflater.inflate(R.layout.delete_meal_button, null) as Button

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        deleteButton.layoutParams = layoutParams

        deleteButton.setOnClickListener {
            mealsList.remove(meal)
            mealContainer.removeView(mealTextView)
            mealContainer.removeView(deleteButton)
            totalCalories -= meal.calories
            updateTotalCalories()
            animateLemon()
        }
        mealContainer.addView(deleteButton)

        updateTotalCalories()
        animateLemon()
    }

    data class Meal(val selectedOption: String, val calories: Int)

    private fun updateTotalCalories() {
        val kcalTextView: TextView = view?.findViewById(R.id.kcalTextView) ?: return
        kcalTextView.text = totalCalories.toString()
    }

    private fun startTimer() {
        timerJob = GlobalScope.launch {
            while (isActive) {
                updateUserData()
                getHeartRate()
                delay(timerInterval)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveUserData()
        // Zaustavi timer kad se pogled uništi kako bi se izbjegla curenja resursa
        timerJob?.cancel()
    }

    private fun updateUserData() {
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
                            "glassCount" to glassCount,
                            "totalCalories" to totalCalories,
                            "weight" to numberPicker.value
                        )

                        // Ažuriraj polja koristeći updateChildren()
                        snapshot.ref.updateChildren(user)

                        // Prekini petlju nakon pronalaska odgovarajućeg korisnika
                        break
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Greška prilikom dohvaćanja podataka", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "Korisnik nije ulogiran", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getHeartRate(){
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
                        val heartRate = snapshot.child("heartRate").value
                        val displayedHeartRate = heartRate?.toString() ?: "- -"
                        bpmTextView.text = displayedHeartRate

                        // Prekini petlju nakon pronalaska odgovarajućeg korisnika
                        break
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Greška prilikom dohvaćanja podataka", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "Korisnik nije ulogiran", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (!currentUserEmail.isNullOrEmpty()) {
            val sharedPreferences = requireActivity().getSharedPreferences(
                "UserData_$currentUserEmail",
                Context.MODE_PRIVATE
            )
            val editor = sharedPreferences.edit()

            editor.putInt("glassCount", glassCount)
            editor.putInt("totalCalories", totalCalories)
            editor.putInt("numberPickerValue", numberPicker.value)
            editor.apply()

            saveMealList(mealsList)
        }
    }

    private fun loadUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (!currentUserEmail.isNullOrEmpty()) {
            val sharedPreferences = requireActivity().getSharedPreferences(
                "UserData_$currentUserEmail",
                Context.MODE_PRIVATE
            )
            val savedGlassCount = sharedPreferences.getInt("glassCount", 0)
            val savedTotalCalories = sharedPreferences.getInt("totalCalories", 0)
            val savedNumberPickerValue = sharedPreferences.getInt("numberPickerValue", 78)

            glassCount = savedGlassCount
            totalCalories = savedTotalCalories
            numberPicker.value = savedNumberPickerValue

            caseCountTextView.text = savedGlassCount.toString()
            updateTotalCalories()
            updateMillilitersTextView()
            animateGlass()
            animateLemon()

            loadMealList()
        }
    }

    private fun saveMealList(meals: List<Meal>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (!currentUserEmail.isNullOrEmpty()) {
            val sharedPreferences = requireActivity().getSharedPreferences(
                "UserData_$currentUserEmail",
                Context.MODE_PRIVATE
            )
            val editor = sharedPreferences.edit()

            val mealsJson = Gson().toJson(meals)
            editor.putString("mealList", mealsJson)
            editor.apply()
        }
    }

    private fun loadMealList() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (!currentUserEmail.isNullOrEmpty()) {
            val sharedPreferences = requireActivity().getSharedPreferences(
                "UserData_$currentUserEmail",
                Context.MODE_PRIVATE
            )
            val mealsJson = sharedPreferences.getString("mealList", "")

            if (!mealsJson.isNullOrEmpty()) {
                val mealsType = object : TypeToken<List<Meal>>() {}.type
                val loadedMeals = Gson().fromJson<List<Meal>>(mealsJson, mealsType)

                mealsList.clear()
                mealsList.addAll(loadedMeals)
                mealContainer.removeAllViews() // Čisti sve postojeće prikazane obroke

                for (meal in loadedMeals) {
                    val mealTextView =
                        layoutInflater.inflate(R.layout.meal_text_view, null) as TextView
                    mealTextView.text = "${meal.selectedOption}: ${meal.calories} kcal"
                    mealContainer.addView(mealTextView)

                    val mealTextViewParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    mealTextViewParams.gravity = Gravity.CENTER_HORIZONTAL
                    mealTextView.layoutParams = mealTextViewParams

                    val deleteButton =
                        layoutInflater.inflate(R.layout.delete_meal_button, null) as Button

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.gravity = Gravity.CENTER_HORIZONTAL
                    deleteButton.layoutParams = layoutParams

                    deleteButton.setOnClickListener {
                        mealsList.remove(meal)
                        mealContainer.removeView(mealTextView)
                        mealContainer.removeView(deleteButton)
                        totalCalories -= meal.calories
                        updateTotalCalories()
                        animateLemon()
                    }
                    mealContainer.addView(deleteButton)
                }
            }
        }
    }
}

class MealDialogFragment : DialogFragment() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var caloriesEditText: EditText
    private lateinit var enterMealButton: Button
    private var mealDialogListener: MealDialogListener? = null
    private lateinit var odustaniButton: Button

    interface MealDialogListener {
        fun onMealEntered(selectedOption: String, calories: Int)
    }

    fun setMealDialogListener(listener: MealDialogListener) {
        mealDialogListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.meal_dialog_fragment_layout, container, false)

        radioGroup = view.findViewById(R.id.radioGroup)
        caloriesEditText = view.findViewById(R.id.caloriesEditText)
        enterMealButton = view.findViewById(R.id.enterMealButton)
        odustaniButton = view.findViewById(R.id.odustaniButton)

        enterMealButton.isEnabled = false

        odustaniButton.setOnClickListener {
            dismiss()
        }

        //Postavljanje slušatelja koji će se aktivirati svaki put kada se promijeni odabrani radio gumb unutar RadioGroup,
        //_,_ se koristi za naznačavanje da se prva i druga vrijednost neće koristiti unutar tijela funkcije
                radioGroup.setOnCheckedChangeListener { _, _ ->
            validateInputs()
        }

        caloriesEditText.setOnKeyListener { _, _, _ ->
            validateInputs()
            false
        }

        enterMealButton.setOnClickListener {
            val selectedRadioButton = view.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
            val selectedOption = selectedRadioButton?.text.toString()
            val calories = caloriesEditText.text.toString().toInt()

            // Pozovi funkciju koja se nalazi u Home fragmentu za unos obroka s odabranim radio gumbom i unosom kalorija
            mealDialogListener?.onMealEntered(selectedOption, calories)
            dismiss()
        }

        return view
    }

    private fun validateInputs() {
        val selectedRadioButton = view?.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
        val calories = caloriesEditText.text.toString()

        enterMealButton.isEnabled = selectedRadioButton != null && calories.isNotEmpty() && isNumeric(calories)
    }

    //\\d+ provjerava da li se string sastoji samo od jednog ili više brojevnih znakova (0-9), što odgovara pozitivnom cijelom broju.
    //str.toIntOrNull() != null  konvertira string u integer, ako je rezultat konverzije različit od null, to znači da je string ispravno konvertiran u cijeli broj.
    //str.toInt() > 0 provjerava je li konvertirani broj veći od nule, što znači da je to pozitivan cijeli broj
    //Ako svi uvjeti budu zadovoljeni, metoda će vratiti true, inače će vratiti false
    private fun isNumeric(str: String): Boolean {
        return str.matches("\\d+".toRegex()) && str.toIntOrNull() != null && str.toInt() > 0
    }
}
