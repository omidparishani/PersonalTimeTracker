package com.personal.timetracker.jira

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.personal.timetracker.util.ThemeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ساخت فرم پویا برای ایجاد/ویرایش Issue بر اساس createmeta/editmeta.
 */
class JiraIssueFormHelper(
    private val ctx: Context,
    private val primary: Int,
    private val dark: Boolean,
    private val scope: CoroutineScope,
    private val service: JiraService,
    private val projectKey: String,
    private val projectId: String? = null,
    private val issueTypeId: String? = null,
    private val currentUserName: String? = null,
    private val currentUserDisplay: String? = null
) {
    private val skip = setOf(
        "project", "issuetype", "reporter", "attachment", "issuelinks",
        "subtasks", "worklog", "comment", "thumbnail", "issuekey", "parent",
        "watches", "votes", "issuerestriction", "timespent", "aggregatetimespent",
        "aggregatetimeoriginalestimate", "aggregatetimeestimate", "workratio",
        "lastViewed", "creator", "progress", "aggregateprogress", "status",
        "resolution", "resolutiondate", "created", "updated", "security"
    )

    data class FieldWidgets(
        val meta: JiraMetaField,
        val kind: Kind,
        var singleSpinner: Spinner? = null,
        var multiSelected: MutableSet<Int> = linkedSetOf(),
        var multiButton: MaterialButton? = null,
        var edit: EditText? = null,
        var pickButton: MaterialButton? = null,
        var pickedValue: Any? = null, // user map / issue key string / date string / SR ids
        var srMeta: ScriptRunnerFieldMeta? = null
    )

    enum class Kind { TEXT, NUMBER, SINGLE, MULTI, USER, DATE, ISSUE_LINK, TIME, SCRIPT_RUNNER }

    private val widgets = linkedMapOf<String, FieldWidgets>()

    fun build(container: LinearLayout, fields: Map<String, JiraMetaField>, existing: Map<String, Any?> = emptyMap()) {
        container.removeAllViews()
        widgets.clear()
        val merged = fields.toMutableMap()
        DemiscoScriptRunnerFields.known.forEach { sr ->
            if (sr.customFieldId !in merged) {
                merged[sr.customFieldId] = JiraMetaField(
                    required = true,
                    name = sr.labelHint.ifBlank { sr.customFieldId },
                    schema = JiraFieldSchema(
                        type = if (sr.multiple) "array" else "string",
                        custom = "com.onresolve.scriptrunner.canned.jira.fields.editable.database.DbPickerCannedField"
                    )
                )
            }
        }
        val entries = merged.entries
            .filter { (k, _) -> k !in skip }
            .sortedWith(
                compareByDescending<Map.Entry<String, JiraMetaField>> { it.value.required }
                    .thenBy {
                        when (it.key) {
                            "summary" -> 0
                            "description" -> 1
                            "assignee" -> 2
                            "priority" -> 3
                            "components" -> 4
                            else -> 10
                        }
                    }
                    .thenBy { it.value.name ?: it.key }
            )
        // ensure estimate fields appear even if nested under timetracking-only
        val ensured = entries.toMutableList()
        if (fields.keys.none { it.equals("originalEstimate", true) || it == "timetracking" }) {
            // still show if company uses string estimates as custom - skip
        }
        ensured.forEach { (key, meta) ->
            addField(container, key, meta, existing[key])
        }
        // timetracking split
        val tt = fields["timetracking"]
        if (tt != null && "originalEstimate" !in widgets) {
            addField(
                container, "originalEstimate",
                JiraMetaField(required = false, name = "Original Estimate", schema = JiraFieldSchema(type = "string", system = "timetracking")),
                null
            )
            addField(
                container, "remainingEstimate",
                JiraMetaField(required = false, name = "Remaining Estimate", schema = JiraFieldSchema(type = "string", system = "timetracking")),
                null
            )
        }
        if ("summary" !in widgets) {
            addField(
                container, "summary",
                JiraMetaField(required = true, name = "Summary", schema = JiraFieldSchema(type = "string", system = "summary")),
                existing["summary"]
            )
        }
    }

    private fun addField(container: LinearLayout, key: String, meta: JiraMetaField, existing: Any?) {
        val kind = detectKind(key, meta)
        val label = buildString {
            append(meta.name ?: key)
            if (meta.required) append(" *")
        }
        container.addView(TextView(ctx).apply {
            text = label
            setTextColor(if (meta.required) primary else ThemeHelper.textSecondary(dark))
            textSize = 12.5f
            setTypeface(null, if (meta.required) Typeface.BOLD else Typeface.NORMAL)
            setPadding(0, 12, 0, 2)
        })
        val fw = FieldWidgets(meta, kind)
        when (kind) {
            Kind.SINGLE -> {
                val allowed = meta.allowedValues.orEmpty()
                val names = mutableListOf<String>()
                if (!meta.required) names.add("— انتخاب کنید —")
                names.addAll(allowed.map { displayAv(it) })
                val sp = Spinner(ctx).apply {
                    adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, names)
                }
                // preselect existing
                fw.singleSpinner = sp
                container.addView(sp)
            }
            Kind.MULTI -> {
                val allowed = meta.allowedValues.orEmpty()
                val btn = MaterialButton(ctx).apply {
                    text = "انتخاب… (۰)"
                    ThemeHelper.applyButton(this, primary, false)
                    setOnClickListener {
                        val labels = allowed.map { displayAv(it) }.toTypedArray()
                        val checked = BooleanArray(labels.size) { fw.multiSelected.contains(it) }
                        AlertDialog.Builder(ctx)
                            .setTitle(meta.name ?: key)
                            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                                if (isChecked) fw.multiSelected.add(which) else fw.multiSelected.remove(which)
                            }
                            .setPositiveButton("تأیید") { _, _ ->
                                text = "انتخاب… (${fw.multiSelected.size})"
                            }
                            .setNegativeButton("انصراف", null)
                            .show()
                    }
                }
                fw.multiButton = btn
                container.addView(btn)
            }
            Kind.USER -> {
                val initial = when {
                    existing != null -> existing.toString()
                    key == "assignee" && !currentUserDisplay.isNullOrBlank() -> currentUserDisplay
                    key == "assignee" && !currentUserName.isNullOrBlank() -> currentUserName
                    else -> null
                }
                if (key == "assignee" && !currentUserName.isNullOrBlank()) {
                    fw.pickedValue = mapOf("name" to currentUserName)
                }
                val btn = MaterialButton(ctx).apply {
                    text = initial?.let { "اساین: $it" } ?: "انتخاب کاربر…"
                    ThemeHelper.applyButton(this, primary, false)
                    setOnClickListener { openUserPicker(key, fw, this) }
                }
                fw.pickButton = btn
                container.addView(btn)
            }
            Kind.DATE -> {
                val btn = MaterialButton(ctx).apply {
                    text = existing?.toString()?.take(10) ?: "انتخاب تاریخ…"
                    ThemeHelper.applyButton(this, primary, false)
                    setOnClickListener {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            ctx,
                            { _, y, m, d ->
                                val v = String.format("%04d-%02d-%02d", y, m + 1, d)
                                fw.pickedValue = v
                                text = v
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                }
                fw.pickButton = btn
                container.addView(btn)
            }
            Kind.ISSUE_LINK -> {
                val btn = MaterialButton(ctx).apply {
                    text = existing?.toString() ?: "جستجوی Issue…"
                    ThemeHelper.applyButton(this, primary, false)
                    setOnClickListener { openIssuePicker(key, fw, this) }
                }
                fw.pickButton = btn
                container.addView(btn)
            }
            Kind.SCRIPT_RUNNER -> {
                val sr = DemiscoScriptRunnerFields.byFieldId(key)
                    ?: ScriptRunnerFieldMeta(key, "", multiple = false, labelHint = meta.name ?: key)
                fw.srMeta = sr
                val btn = MaterialButton(ctx).apply {
                    text = "انتخاب ${meta.name ?: key}…"
                    ThemeHelper.applyButton(this, primary, false)
                    setOnClickListener { openScriptRunnerPicker(key, fw, this) }
                }
                fw.pickButton = btn
                container.addView(btn)
            }
            Kind.TIME, Kind.NUMBER, Kind.TEXT -> {
                val multi = key == "description" || meta.schema?.system == "description"
                val et = TextInputEditText(ctx).apply {
                    hint = when (kind) {
                        Kind.TIME -> "مثلاً 2h 30m"
                        Kind.NUMBER -> "عدد"
                        else -> meta.name ?: key
                    }
                    if (kind == Kind.NUMBER) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    if (multi) {
                        minLines = 3
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    }
                    if (existing != null) setText(existing.toString())
                }
                fw.edit = et
                container.addView(TextInputLayout(ctx).apply {
                    hint = if (meta.required) "${meta.name ?: key} *" else (meta.name ?: key)
                    addView(et)
                })
            }
        }
        widgets[key] = fw
    }

    private fun detectKind(key: String, meta: JiraMetaField): Kind {
        val t = meta.schema?.type.orEmpty()
        val items = meta.schema?.items.orEmpty()
        val system = meta.schema?.system.orEmpty()
        val custom = meta.schema?.custom.orEmpty()
        val name = (meta.name ?: key).lowercase()
        val allowed = meta.allowedValues.orEmpty()

        // ScriptRunner DB Picker (ActivityType / BudgetType / DemisCustomer)
        if (DemiscoScriptRunnerFields.byFieldId(key) != null ||
            custom.contains("scriptrunner", true) ||
            custom.contains("onresolve", true) ||
            custom.contains("DbPicker", true) ||
            name.contains("activitytype") || name.contains("budgettype") || name.contains("demiscustomer")
        ) {
            // اگر allowedValues از createmeta آمده، همان select معمولی
            if (allowed.isEmpty()) return Kind.SCRIPT_RUNNER
        }

        if (key == "assignee" || t == "user" || items == "user") return Kind.USER
        if (t == "date" || t == "datetime" || system == "duedate" || name.contains("date")) return Kind.DATE
        if (key in setOf("originalEstimate", "remainingEstimate") || system == "timetracking" && t != "array") {
            if (key.contains("estimate", true) || name.contains("estimate")) return Kind.TIME
        }
        if (key == "timetracking") return Kind.TIME
        // epic / sprint / parent link
        if (custom.contains("gh-epic-link", true) || custom.contains("epic", true) ||
            name.contains("epic") || key.equals("parent", true) ||
            custom.contains("gh-sprint", true) || name.contains("sprint")
        ) return Kind.ISSUE_LINK
        // هر فیلدی که allowedValues دارد (از جمله customfieldهای شرکت مثل BudgetType)
        if (allowed.isNotEmpty()) {
            return if (t == "array" || items in setOf("option", "component", "version")) Kind.MULTI
            else Kind.SINGLE
        }
        if (t == "array" && items in setOf("option", "component", "version")) {
            return Kind.MULTI
        }
        if (t == "number") return Kind.NUMBER
        return Kind.TEXT
    }

    private fun displayAv(av: JiraAllowedValue): String =
        av.name ?: av.value ?: av.key ?: av.id ?: "?"

    private fun openUserPicker(key: String, fw: FieldWidgets, btn: MaterialButton) {
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val q = TextInputEditText(ctx).apply { hint = "نام کاربری یا نمایشی…" }
        box.addView(TextInputLayout(ctx).apply { hint = "جستجو"; addView(q) })
        val listBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        box.addView(listBox)
        val dlg = AlertDialog.Builder(ctx)
            .setTitle("انتخاب کاربر")
            .setView(box)
            .setNegativeButton("بستن", null)
            .create()
        fun runSearch() {
            scope.launch {
                val query = q.text?.toString().orEmpty()
                service.searchAssignableUsers(projectKey, query).fold(
                    onSuccess = { users ->
                        listBox.removeAllViews()
                        if (users.isEmpty()) {
                            listBox.addView(TextView(ctx).apply { text = "موردی نیست" })
                        } else {
                            users.forEach { u ->
                                val label = u.displayName ?: u.name ?: u.key ?: "?"
                                listBox.addView(MaterialButton(ctx).apply {
                                    text = label
                                    ThemeHelper.applyButton(this, primary, false)
                                    setOnClickListener {
                                        val name = u.name ?: u.key
                                        fw.pickedValue = if (!name.isNullOrBlank()) mapOf("name" to name)
                                        else mapOf("name" to label)
                                        btn.text = "اساین: $label"
                                        dlg.dismiss()
                                    }
                                })
                            }
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
        q.setOnEditorActionListener { _, _, _ -> runSearch(); true }
        box.addView(MaterialButton(ctx).apply {
            text = "جستجو"
            ThemeHelper.applyButton(this, primary, true)
            setOnClickListener { runSearch() }
        })
        // default: load without query
        runSearch()
        dlg.show()
    }


    private fun openScriptRunnerPicker(key: String, fw: FieldWidgets, btn: MaterialButton) {
        val sr = fw.srMeta ?: DemiscoScriptRunnerFields.byFieldId(key)
        if (sr == null || sr.fcsId.isBlank()) {
            Toast.makeText(ctx, "پیکربندی ScriptRunner برای این فیلد مشخص نیست", Toast.LENGTH_LONG).show()
            return
        }
        val pid = projectId
        val typeId = issueTypeId
        if (pid.isNullOrBlank() || typeId.isNullOrBlank()) {
            Toast.makeText(ctx, "شناسه پروژه/نوع Issue برای بارگذاری گزینه‌ها لازم است", Toast.LENGTH_LONG).show()
            return
        }
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val q = TextInputEditText(ctx).apply { hint = "جستجو…" }
        box.addView(TextInputLayout(ctx).apply { hint = "جستجو"; addView(q) })
        val listBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        box.addView(
            android.widget.ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (ctx.resources.displayMetrics.density * 280).toInt()
                )
                addView(listBox)
            }
        )
        val selectedIds = linkedSetOf<String>()
        @Suppress("UNCHECKED_CAST")
        (fw.pickedValue as? List<*>)?.mapNotNull { it as? String }?.let { selectedIds.addAll(it) }
        if (fw.pickedValue is String) selectedIds.add(fw.pickedValue as String)

        val dlg = AlertDialog.Builder(ctx)
            .setTitle(fw.meta.name ?: key)
            .setView(box)
            .setPositiveButton("تأیید") { _, _ ->
                if (sr.multiple) {
                    fw.pickedValue = selectedIds.toList()
                    btn.text = if (selectedIds.isEmpty()) "انتخاب…" else "انتخاب‌شده: ${selectedIds.size}"
                } else {
                    val one = selectedIds.firstOrNull()
                    fw.pickedValue = one
                    btn.text = one?.let { "✓ $it" } ?: "انتخاب…"
                }
            }
            .setNegativeButton("انصراف", null)
            .create()

        fun runSearch() {
            scope.launch {
                listBox.removeAllViews()
                listBox.addView(TextView(ctx).apply {
                    text = "در حال بارگذاری…"
                    setTextColor(ThemeHelper.textSecondary(dark))
                })
                service.searchScriptRunnerPicker(
                    fcsId = sr.fcsId,
                    projectId = pid,
                    issueTypeId = typeId,
                    inputValue = q.text?.toString().orEmpty()
                ).fold(
                    onSuccess = { items ->
                        listBox.removeAllViews()
                        if (items.isEmpty()) {
                            listBox.addView(TextView(ctx).apply {
                                text = "موردی نیست — متن جستجو را تغییر دهید"
                                setTextColor(ThemeHelper.textSecondary(dark))
                            })
                        } else {
                            items.forEach { item ->
                                listBox.addView(MaterialButton(ctx).apply {
                                    val mark = if (item.id in selectedIds) "✓ " else ""
                                    text = "$mark${item.label}"
                                    ThemeHelper.applyButton(this, primary, item.id in selectedIds)
                                    setOnClickListener {
                                        if (sr.multiple) {
                                            if (item.id in selectedIds) selectedIds.remove(item.id)
                                            else selectedIds.add(item.id)
                                            // refresh marks
                                            runSearch()
                                        } else {
                                            selectedIds.clear()
                                            selectedIds.add(item.id)
                                            fw.pickedValue = item.id
                                            btn.text = "✓ ${item.label}"
                                            dlg.dismiss()
                                        }
                                    }
                                })
                            }
                        }
                    },
                    onFailure = { e ->
                        listBox.removeAllViews()
                        listBox.addView(TextView(ctx).apply {
                            text = "خطا: ${e.message}"
                            setTextColor(0xFFE53935.toInt())
                        })
                    }
                )
            }
        }
        box.addView(MaterialButton(ctx).apply {
            text = "جستجو"
            ThemeHelper.applyButton(this, primary, true)
            setOnClickListener { runSearch() }
        })
        runSearch()
        dlg.show()
    }

    private fun openIssuePicker(key: String, fw: FieldWidgets, btn: MaterialButton) {
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val q = TextInputEditText(ctx).apply { hint = "کلید یا عنوان…" }
        box.addView(TextInputLayout(ctx).apply { hint = "جستجو"; addView(q) })
        val listBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        box.addView(listBox)
        val dlg = AlertDialog.Builder(ctx)
            .setTitle(fw.meta.name ?: "انتخاب Issue")
            .setView(box)
            .setNegativeButton("بستن", null)
            .create()
        fun runSearch() {
            val query = q.text?.toString().orEmpty()
            if (query.isBlank()) {
                Toast.makeText(ctx, "متن جستجو را وارد کنید", Toast.LENGTH_SHORT).show()
                return
            }
            scope.launch {
                service.searchIssuesPicker(query, projectKey).fold(
                    onSuccess = { items ->
                        listBox.removeAllViews()
                        if (items.isEmpty()) {
                            listBox.addView(TextView(ctx).apply { text = "موردی نیست" })
                        } else {
                            items.forEach { (issueKey, summary) ->
                                listBox.addView(MaterialButton(ctx).apply {
                                    text = "$issueKey — ${summary.take(60)}"
                                    ThemeHelper.applyButton(this, primary, false)
                                    setOnClickListener {
                                        // epic link often wants just key string; parent wants key map
                                        fw.pickedValue = when {
                                            key.equals("parent", true) -> mapOf("key" to issueKey)
                                            else -> issueKey
                                        }
                                        btn.text = "$issueKey"
                                        dlg.dismiss()
                                    }
                                })
                            }
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
        box.addView(MaterialButton(ctx).apply {
            text = "جستجو"
            ThemeHelper.applyButton(this, primary, true)
            setOnClickListener { runSearch() }
        })
        dlg.show()
    }

    /**
     * جمع‌آوری فیلدها برای ارسال به API.
     * @return null اگر اعتبارسنجی فیلد اجباری شکست بخورد (Toast نشان داده شده)
     */
    fun collect(): Map<String, Any?>? {
        val out = mutableMapOf<String, Any?>()
        var originalEst: String? = null
        var remainingEst: String? = null
        for ((key, fw) in widgets) {
            val meta = fw.meta
            when (fw.kind) {
                Kind.TEXT, Kind.NUMBER, Kind.TIME -> {
                    val v = fw.edit?.text?.toString()?.trim().orEmpty()
                    if (v.isEmpty()) {
                        if (meta.required || key == "summary") {
                            Toast.makeText(ctx, "${meta.name ?: key} الزامی است", Toast.LENGTH_SHORT).show()
                            return null
                        }
                        continue
                    }
                    when {
                        key == "originalEstimate" || key.equals("originalestimate", true) -> originalEst = v
                        key == "remainingEstimate" || key.equals("remainingestimate", true) -> remainingEst = v
                        key == "labels" || (meta.schema?.type == "array" && meta.schema?.items == "string") ->
                            out[key] = v.split(",", " ", "،").map { it.trim() }.filter { it.isNotEmpty() }
                        fw.kind == Kind.NUMBER -> out[key] = v.toDoubleOrNull() ?: v
                        key == "timetracking" -> {
                            // plain string not valid; ignore
                        }
                        else -> out[key] = v
                    }
                }
                Kind.SINGLE -> {
                    val sp = fw.singleSpinner ?: continue
                    val allowed = meta.allowedValues.orEmpty()
                    var pos = sp.selectedItemPosition
                    if (!meta.required) {
                        if (pos <= 0) continue
                        pos -= 1
                    }
                    val av = allowed.getOrNull(pos)
                    if (av == null) {
                        if (meta.required) {
                            Toast.makeText(ctx, "${meta.name ?: key} الزامی است", Toast.LENGTH_SHORT).show()
                            return null
                        }
                        continue
                    }
                    out[key] = encodeAv(key, meta, listOf(av), multi = false)
                }
                Kind.MULTI -> {
                    val allowed = meta.allowedValues.orEmpty()
                    val selected = fw.multiSelected.mapNotNull { allowed.getOrNull(it) }
                    if (selected.isEmpty()) {
                        if (meta.required) {
                            Toast.makeText(ctx, "${meta.name ?: key} الزامی است", Toast.LENGTH_SHORT).show()
                            return null
                        }
                        continue
                    }
                    out[key] = encodeAv(key, meta, selected, multi = true)
                }
                Kind.USER, Kind.DATE, Kind.ISSUE_LINK -> {
                    val v = fw.pickedValue
                    if (v == null) {
                        if (meta.required || (key == "assignee" && meta.required)) {
                            Toast.makeText(ctx, "${meta.name ?: key} الزامی است", Toast.LENGTH_SHORT).show()
                            return null
                        }
                        continue
                    }
                    out[key] = v
                }
                Kind.SCRIPT_RUNNER -> {
                    val v = fw.pickedValue
                    if (v == null || (v is List<*> && v.isEmpty()) || (v is String && v.isBlank())) {
                        if (meta.required) {
                            Toast.makeText(ctx, "${meta.name ?: key} الزامی است", Toast.LENGTH_SHORT).show()
                            return null
                        }
                        continue
                    }
                    // ScriptRunner DB picker معمولاً id رشته‌ای یا آرایه id می‌پذیرد
                    out[key] = v
                }
            }
        }
        if (originalEst != null || remainingEst != null) {
            val tt = mutableMapOf<String, String>()
            originalEst?.let { tt["originalEstimate"] = it }
            remainingEst?.let { tt["remainingEstimate"] = it }
            out["timetracking"] = tt
            out.remove("originalEstimate")
            out.remove("remainingEstimate")
        }
        return out
    }

    private fun encodeAv(key: String, meta: JiraMetaField, selected: List<JiraAllowedValue>, multi: Boolean): Any {
        val t = meta.schema?.type.orEmpty()
        val items = meta.schema?.items.orEmpty()
        fun one(av: JiraAllowedValue): Map<String, String> = when {
            av.id != null && av.value != null -> mapOf("id" to av.id!!, "value" to av.value!!)
            av.id != null -> mapOf("id" to av.id!!)
            av.value != null -> mapOf("value" to av.value!!)
            av.name != null -> mapOf("name" to av.name!!)
            av.key != null -> mapOf("key" to av.key!!)
            else -> mapOf("value" to "?")
        }
        val list = selected.map { one(it) }
        return if (multi || t == "array" || items.isNotEmpty()) list else list.first()
    }
}
