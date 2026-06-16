import org.jsoup.Jsoup
import io.github.bonigarcia.wdm.WebDriverManager
import com.codeborne.selenide.Configuration

import static com.codeborne.selenide.Selenide.*
import static com.codeborne.selenide.Condition.exist

class ChallengeExporter {

    static void main(String[] args) {
        run()
    }

    static void run() {
        String baseUrl = "https://cmdchallenge.com/"

        // Browser setup
        WebDriverManager.chromedriver().setup()
        Configuration.baseUrl = baseUrl
        Configuration.holdBrowserOpen = false

        // Disable Selenide reports
        Configuration.reportsFolder = null
        Configuration.downloadsFolder = null
        Configuration.screenshots = false
        Configuration.savePageSource = false

        open(baseUrl)

        Thread.sleep(2000)

        def correctAnswers = '["hello_world","current_working_directory","list_files","print_file_contents","last_lines","create_file","create_directory","copy_file","move_file","create_symlink","delete_files","remove_files_with_extension","find_string_in_a_file","search_for_files_containing_string","search_for_files_by_extension","search_for_string_in_files_recursive","extract_ip_addresses","count_files","simple_sort","count_string_in_line","split_on_a_char","print_number_sequence","replace_text_in_files","sum_all_numbers","just_the_files","remove_extensions_from_files","replace_spaces_in_filenames","dirs_containing_files_with_extension","files_starting_with_a_number","print_nth_line","reverse_readme","remove_duplicate_lines","find_primes","print_common_lines","print_line_before","print_files_if_different","nested_dirs","find_tabs_in_a_file","remove_files_without_extension","remove_files_with_a_dash","print_sorted_by_key","IPv4_listening_ports"]'

        executeJavaScript("localStorage.setItem('correct_answers', '${correctAnswers}')")
        executeJavaScript("window.location.reload()")
        Thread.sleep(2000)

        def tasks = $$("a[href*='#/']")
                .collect { link ->
                    [
                            id  : link.attr("href").replaceAll('.*#/', ''),
                            name: link.attr("title"),
                            img : link.find("img").attr("src")
                    ]
                }
                .findAll { it.id }
                .unique { it.id }

        println "Found tasks: ${tasks.size()}"

        tasks.eachWithIndex { task, index ->

            println "Processing: ${task.id}"

            openTask(task.id)

            waitForLoad()

            def name = clean(task.id)

            def base = new File("output/${index}_${name}")
            def challengeDir = new File(base, "challenge")
            def solutionsDir = new File(base, "solutions")

            challengeDir.mkdirs()
            solutionsDir.mkdirs()

            // --- README ---
            def challengeText = $("#challenge-desc").text()

            new File(base, "README.md").text = """\
## ${task.name}

<img src="task.png" width="36" alt="image">

${challengeText}
            """.stripIndent()

            Thread.sleep(1000)

            // --- solutions ---
            def solutionsHtml = $("#solutions").getAttribute("outerHTML")
            // Replace newlines in HTML with a placeholder before Jsoup flattens them
            def doc = Jsoup.parse(solutionsHtml.replaceAll('\n', '|||NL|||'))
            def raw = doc.select("code#solutions").first().text()
            def solutionsText = raw.replaceAll('\\|\\|\\|NL\\|\\|\\|', '\n')

            new File(solutionsDir, "solutions.txt").text = solutionsText

            // --- image ---
            def img = $("#challenge-desc img")?.attr("src")
            if (img) {
                downloadImage(img, new File(base, "task.png"))
            }
        }
    }

    static void openTask(String taskId) {
        executeJavaScript("window.location.hash = '#/${taskId}'")
    }

    static void waitForLoad() {
        $("#solutions").should(exist)
    }

    static String clean(String s) {
        s.replaceAll(/[^a-z0-9_]+/, "_")
    }

    static void downloadImage(String url, File file) {
        new URL(url).withInputStream { i ->
            file.withOutputStream { o -> o << i }
        }
    }
}
