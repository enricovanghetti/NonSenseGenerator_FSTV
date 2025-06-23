console.log("JS loaded"); //debug iniziale

function onCopyClick() {
  const output = document.getElementById('outputText').innerText;
  const button = document.getElementById('copyBtn');
  const originalText = 'Copy output';
  const copiedText = '✓ Copied'; //testo temporaneo

  button.innerText = copiedText;
  button.style.width = button.offsetWidth + 'px';

  navigator.clipboard.writeText(output).then(() => {
    setTimeout(() => {
      button.innerText = originalText; //ripristina testo
    }, 2000);
  });
}

window.addEventListener("load", () => {
  const isInitialLoad = document.body.getAttribute("data-init") === "true";
  const titleEl = document.getElementById("title");

  if (isInitialLoad && titleEl) {
    const finalTitle = "NonSenseGenerator";
    const scrambled = "erenaetNeGsnonrSo"; //titolo disordinato

    titleEl.innerHTML = "";
    scrambled.split("").forEach((char) => {
      const span = document.createElement("span");
      span.textContent = char;
      span.classList.add("scramble");
      titleEl.appendChild(span); //animazione iniziale
    });

    setTimeout(() => {
      const spans = document.querySelectorAll(".scramble");
      finalTitle.split("").forEach((targetChar, idx) => {
        const span = spans[idx];
        setTimeout(() => {
          span.textContent = targetChar; //lettere corrette
        }, idx * 250);
      });
    }, 650);
  }

  const outputBox = document.getElementById("outputBox");
  const outputText = document.getElementById("outputText");
  if (outputBox && outputText && outputText.innerText.trim() !== "") {
    outputBox.classList.remove("glow-animation");
    void outputBox.offsetWidth;
    outputBox.classList.add("glow-animation"); //effetto animato
  }

  // pulsante syntax tree
  const treeButton = document.getElementById("generateTreeBtn");
  if (treeButton) {
    treeButton.addEventListener("click", () => {
      const inputText = document.getElementById("inputText").value;
      if (!inputText.trim()) return;

      treeButton.disabled = true;
      const originalText = treeButton.innerText;
      treeButton.innerText = "Generating...";

      fetch("/generateTree", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ inputText })
      }).then(res => res.text())
        .then(data => {
          if (data === "OK") {
            const timestamp = new Date().getTime();
            const treeImage = document.getElementById("treeImg");
            const treeContainer = document.getElementById("treeImageContainer");

            if (treeImage && treeContainer) {
              treeImage.src = "/tree.png?_=" + timestamp; //forza refresh
              treeImage.style.display = "block";
              treeContainer.style.display = "block";
            }
          } else {
            alert("Errore durante la generazione dell'albero sintattico.");
          }
        })
        .catch(err => {
          console.error(err);
          alert("Errore imprevisto."); //errore fetch
        })
        .finally(() => {
          treeButton.disabled = false;
          treeButton.innerText = originalText;
        });
    });
  }

  // pulsante generate output
  const generateBtn = document.getElementById("generateBtn");
  if (generateBtn) {
    generateBtn.addEventListener("click", () => {
      const inputText = document.getElementById("inputText").value;
      const sentenceCount = document.getElementById("sentenceCount").value;

      const formData = new URLSearchParams();
      formData.append("inputText", inputText);
      formData.append("sentenceCount", sentenceCount);

      generateBtn.disabled = true;
      const originalText = generateBtn.innerText;
      generateBtn.innerText = "Generating...";

      fetch("/generate", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formData.toString()
      })
      .then(response => response.text())
      .then(html => {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, "text/html");

        const newOutput = doc.querySelector("#outputText");
        const outputBox = document.querySelector("#outputText");

        if (newOutput && outputBox) {
          outputBox.innerHTML = newOutput.innerHTML;
          const box = document.getElementById("outputBox");
          box.classList.remove("glow-animation");
          void box.offsetWidth;
          box.classList.add("glow-animation"); //effetto visivo
        }
      })
      .catch(err => {
        console.error("Error:", err);
        alert("Failed to generate sentence.");
      })
      .finally(() => {
        generateBtn.disabled = false;
        generateBtn.innerText = originalText;
      });
    });
  }
});
