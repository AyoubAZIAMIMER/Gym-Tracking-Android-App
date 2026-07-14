// Purpose: Prebuilt workout programs (PPL, Upper/Lower, Bro Split, Full Body)
// Inputs: none — exercise names must match assets/exercise_catalog.json exactly
// Outputs: templates instantiated into user programs by WorkoutRepository.createFromTemplate
package com.gymtracker.data

object ProgramTemplates {

    data class TemplateExercise(val exerciseName: String, val sets: Int, val repMin: Int, val repMax: Int)
    data class TemplateDay(val name: String, val exercises: List<TemplateExercise>)
    data class Template(val name: String, val description: String, val days: List<TemplateDay>)

    private fun ex(name: String, sets: Int, min: Int, max: Int) = TemplateExercise(name, sets, min, max)

    val pushPullLegs = Template(
        name = "Push Pull Legs",
        description = "The classic 3-day split — run it 3 or 6 days a week.",
        days = listOf(
            TemplateDay("Push", listOf(
                ex("Bench Press (Barbell)", 4, 6, 10),
                ex("Overhead Press (Barbell)", 3, 8, 12),
                ex("Incline Press (Dumbbell)", 3, 8, 12),
                ex("Lateral Raise (Dumbbell)", 3, 12, 15),
                ex("Triceps Pushdown (Cable)", 3, 10, 15),
                ex("Overhead Triceps Extension (Cable)", 3, 10, 15),
            )),
            TemplateDay("Pull", listOf(
                ex("Deadlift (Barbell)", 3, 5, 8),
                ex("Lat Pulldown (Cable)", 4, 8, 12),
                ex("Seated Row (Cable)", 3, 8, 12),
                ex("Face Pull (Cable)", 3, 12, 20),
                ex("Bicep Curl (Barbell)", 3, 8, 12),
                ex("Hammer Curl (Dumbbell)", 3, 10, 15),
            )),
            TemplateDay("Legs", listOf(
                ex("Squat (Barbell)", 4, 6, 10),
                ex("Romanian Deadlift (Barbell)", 3, 8, 12),
                ex("Leg Press", 3, 10, 15),
                ex("Leg Curl (Machine)", 3, 10, 15),
                ex("Calf Raise (Standing)", 4, 10, 15),
                ex("Cable Crunch", 3, 10, 15),
            )),
        ),
    )

    val upperLower = Template(
        name = "Upper / Lower",
        description = "4-day split alternating upper and lower body.",
        days = listOf(
            TemplateDay("Upper A", listOf(
                ex("Bench Press (Barbell)", 4, 6, 10),
                ex("Bent-Over Row (Barbell)", 4, 8, 12),
                ex("Overhead Press (Dumbbell)", 3, 8, 12),
                ex("Lat Pulldown (Cable)", 3, 8, 12),
                ex("Bicep Curl (Dumbbell)", 3, 10, 15),
                ex("Triceps Pushdown (Cable)", 3, 10, 15),
            )),
            TemplateDay("Lower A", listOf(
                ex("Squat (Barbell)", 4, 6, 10),
                ex("Romanian Deadlift (Barbell)", 3, 8, 12),
                ex("Leg Press", 3, 10, 15),
                ex("Leg Curl (Machine)", 3, 10, 15),
                ex("Calf Raise (Standing)", 4, 10, 15),
            )),
            TemplateDay("Upper B", listOf(
                ex("Incline Press (Barbell)", 4, 6, 10),
                ex("Seated Row (Cable)", 4, 8, 12),
                ex("Arnold Press (Dumbbell)", 3, 8, 12),
                ex("Chin-Up", 3, 6, 10),
                ex("EZ-Bar Curl", 3, 8, 12),
                ex("Skullcrusher (EZ-Bar)", 3, 10, 12),
            )),
            TemplateDay("Lower B", listOf(
                ex("Deadlift (Barbell)", 3, 5, 8),
                ex("Front Squat (Barbell)", 3, 6, 10),
                ex("Bulgarian Split Squat", 3, 8, 12),
                ex("Hip Thrust (Barbell)", 3, 8, 12),
                ex("Calf Raise (Seated)", 4, 10, 15),
                ex("Hanging Leg Raise", 3, 8, 15),
            )),
        ),
    )

    val broSplit = Template(
        name = "Bro Split",
        description = "5-day body-part split — one muscle group per day, maximum pump.",
        days = listOf(
            TemplateDay("Chest", listOf(
                ex("Bench Press (Barbell)", 4, 6, 10),
                ex("Incline Press (Dumbbell)", 3, 8, 12),
                ex("Cable Fly", 3, 12, 15),
                ex("Dips (Chest)", 3, 8, 12),
                ex("Push-Up", 2, 15, 20),
            )),
            TemplateDay("Back", listOf(
                ex("Deadlift (Barbell)", 3, 5, 8),
                ex("Pull-Up", 4, 6, 10),
                ex("T-Bar Row", 3, 8, 12),
                ex("Straight-Arm Pulldown (Cable)", 3, 12, 15),
                ex("Shrug (Dumbbell)", 3, 10, 15),
            )),
            TemplateDay("Shoulders", listOf(
                ex("Overhead Press (Barbell)", 4, 6, 10),
                ex("Lateral Raise (Dumbbell)", 4, 12, 15),
                ex("Rear Delt Fly (Dumbbell)", 3, 12, 15),
                ex("Front Raise (Dumbbell)", 3, 10, 15),
                ex("Face Pull (Cable)", 3, 15, 20),
            )),
            TemplateDay("Arms", listOf(
                ex("Close-Grip Bench Press", 4, 8, 12),
                ex("EZ-Bar Curl", 4, 8, 12),
                ex("Skullcrusher (EZ-Bar)", 3, 10, 12),
                ex("Hammer Curl (Dumbbell)", 3, 10, 15),
                ex("Triceps Pushdown (Cable)", 3, 12, 15),
                ex("Cable Curl", 3, 12, 15),
            )),
            TemplateDay("Legs", listOf(
                ex("Squat (Barbell)", 4, 6, 10),
                ex("Leg Press", 3, 10, 15),
                ex("Leg Curl (Machine)", 3, 10, 15),
                ex("Leg Extension (Machine)", 3, 12, 15),
                ex("Calf Raise (Standing)", 4, 10, 15),
            )),
        ),
    )

    val fullBody = Template(
        name = "Full Body",
        description = "3-day full-body — high frequency, great for beginners and time-crunched lifters.",
        days = listOf(
            TemplateDay("Full Body A", listOf(
                ex("Squat (Barbell)", 3, 6, 10),
                ex("Bench Press (Barbell)", 3, 6, 10),
                ex("Seated Row (Cable)", 3, 8, 12),
                ex("Lateral Raise (Dumbbell)", 3, 12, 15),
                ex("Cable Crunch", 3, 10, 15),
            )),
            TemplateDay("Full Body B", listOf(
                ex("Deadlift (Barbell)", 3, 5, 8),
                ex("Overhead Press (Barbell)", 3, 6, 10),
                ex("Lat Pulldown (Cable)", 3, 8, 12),
                ex("Leg Curl (Machine)", 3, 10, 15),
                ex("Bicep Curl (Dumbbell)", 3, 10, 15),
            )),
            TemplateDay("Full Body C", listOf(
                ex("Front Squat (Barbell)", 3, 6, 10),
                ex("Incline Press (Dumbbell)", 3, 8, 12),
                ex("One-Arm Row (Dumbbell)", 3, 8, 12),
                ex("Hip Thrust (Barbell)", 3, 8, 12),
                ex("Triceps Pushdown (Cable)", 3, 10, 15),
            )),
        ),
    )

    val all = listOf(pushPullLegs, upperLower, broSplit, fullBody)
}
