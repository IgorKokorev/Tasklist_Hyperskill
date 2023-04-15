package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

val listOfTasks = ListOfTasks()

val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

//val taskAdapter = moshi.adapter(Task::class.java)
val type = Types.newParameterizedType(List::class.java, Task::class.java)
val taskListAdapter = moshi.adapter<MutableList<Task>>(type)

fun main() {
    val jsonFile = File("tasklist.json")

    if (jsonFile.exists()) {
        val readFile = jsonFile.readText()
        val dbFromJson = taskListAdapter.fromJson(readFile)
        if (dbFromJson != null) {
            for (rec in dbFromJson) {
                listOfTasks.tasks.add(rec)
            }
        }
    }

    menu()

    jsonFile.writeText(taskListAdapter.toJson(listOfTasks.tasks))
}

fun menu() {
    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (readln().lowercase()) {
            "add" -> listOfTasks.addTask()
            "print" -> listOfTasks.printTasks()
            "edit" -> listOfTasks.editTask()
            "delete" -> listOfTasks.deleteTasks()
            "end" -> break
            else -> println("The input action is invalid")
        }
    }
    println("Tasklist exiting!")
}



class ListOfTasks() {
    val tasks = mutableListOf<Task>()

    private val NO_TASK = "No tasks have been input"

    fun addTask() {

        val priority: String = getPriority()
        val date: String = getDate()
        val time: String = getTime()
        val lines = getLines()

        if (lines.isEmpty()) println("The task is blank")
        else tasks.add(Task(priority, date, time, lines))
    }

    private fun getPriority(): String {
        var priority: String
        while (true) {
            println("Input the task priority (C, H, N, L):")
            priority = readln().uppercase()
            if (Regex("[CHNL]").matches(priority)) break
        }
        return priority
    }

    private fun getDate(): String {
        var date: String
        while (true) {
            println("Input the date (yyyy-mm-dd):")
            date = readln()
            if (Regex("""\d+-\d+-\d+""").matches(date)) {
                val split = date.split('-')
                try {
                    val year = split[0].toInt()
                    val month = split[1].toInt()
                    val day = split[2].toInt()
                    date = LocalDate(year, month, day).toString()
                    break
                } catch (_: IllegalArgumentException) {
                }
            }
            println("The input date is invalid")
        }
        return date
    }

    private fun getTime(): String {
        var time: String
        while (true) {
            println("Input the time (hh:mm):")
            time = readln()
            val split = time.split(':')
            if (Regex("\\d+:\\d+").matches(time)) {
                if (split[0].toInt() in 0..23 &&
                    split[1].toInt() in 0..59
                ) {
                    time = "%02d:%02d".format(split[0].toInt(), split[1].toInt())
                    break
                }
            }
            println("The input time is invalid")
        }
        return time
    }

    private fun getLines(): MutableList<String> {
        val lines = mutableListOf<String>()
        println("Input a new task (enter a blank line to end):")
        while (true) {
            val nextLine = readln().trim()
            if (nextLine.equals("")) break
            lines.add(nextLine)
        }
        return lines
    }

    // returns true if list is printed, false if list is empty
    fun printTasks(): Boolean {
        if (tasks.isEmpty()) {
            println(NO_TASK)
            return false
        }
        println("+----+------------+-------+---+---+--------------------------------------------+\n" +
                "| N  |    Date    | Time  | P | D |                   Task                     |\n" +
                "+----+------------+-------+---+---+--------------------------------------------+")

        for (i in tasks.indices) tasks[i].printTask(i)
        return true
    }

    fun editTask() {
        if (!this.printTasks()) return

        val numTask: Int = getTaskNum()

        var field: String
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            field = readln().lowercase()

            when (field) {
                "priority" -> this.tasks[numTask].priority = getPriority()
                "date" -> this.tasks[numTask].date = getDate()
                "time" -> this.tasks[numTask].time = getTime()
                "task" -> this.tasks[numTask].lines = getLines()
                else -> {
                    println("Invalid field")
                    continue
                }
            }

            if (this.tasks[numTask].lines.isEmpty()) {
                tasks.removeAt(numTask)
                println("The task is deleted")
            }
            else println("The task is changed")
            break
        }
    }

    fun deleteTasks() {
        if (!this.printTasks()) return

        val numTask: Int = getTaskNum()

        tasks.removeAt(numTask)
        println("The task is deleted")
    }

    private fun getTaskNum(): Int {
        var numTask: Int
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            try {
                numTask = readln().toInt() - 1
            } catch (e: NumberFormatException) {
                println("Invalid task number")
                continue
            }
            if (numTask !in 0 until tasks.size) {
                println("Invalid task number")
                continue
            }
            break
        }
        return numTask
    }
}

class Task(priority: String, date: String, time: String, lines: MutableList<String>) {
    var priority: String
    var date: String
    var time: String
    var lines: MutableList<String>

    init {
        this.priority = priority
        this.date = date
        this.time = time
        this.lines = lines
    }

    fun printTask(n: Int) {

        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
        val nDays = currentDate.daysUntil(LocalDate.parse(date))
        val dueTag = if (nDays == 0) "T"
        else if (nDays > 0) "I"
        else "O"

        //print first line
        print("| %-3d".format(n + 1))
        print("| $date ")
        print("| $time ")
        when(priority) {
            "C" -> print("| \u001B[101m \u001B[0m ")
            "H" -> print("| \u001B[103m \u001B[0m ")
            "N" -> print("| \u001B[102m \u001B[0m ")
            "L" -> print("| \u001B[104m \u001B[0m ")
        }
        when(dueTag) {
            "I" -> print("| \u001B[102m \u001B[0m ")
            "T" -> print("| \u001B[103m \u001B[0m ")
            "O" -> print("| \u001B[101m \u001B[0m ")
        }
        println("|%-44s|".format(this.lines[0].take(44)))
        if (this.lines[0].length > 44) printTaskLine(this.lines[0].substring(44))



        for (i in 1..lines.lastIndex) printTaskLine(this.lines[i])

        println("+----+------------+-------+---+---+--------------------------------------------+")
    }

    private fun printTaskLine(str: String) {
        println("|    |            |       |   |   |%-44s|".format(str.take(44)))
        if (str.length > 44) printTaskLine(str.substring(44))
    }
}