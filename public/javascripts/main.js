document.addEventListener('DOMContentLoaded', (event) => {
    delaySuccess();
});
const year = document.querySelector('#current-year');
year.innerHTML = new Date().getFullYear();

document.querySelector(".goBack").addEventListener("click", function() {
    window.history.go(-1);
});

document.getElementById('details-btn').addEventListener('click', function() {
    var details = document.getElementById('error-details');
    if (details.classList.contains('hidden')) {
        details.classList.remove('hidden');
        this.textContent = 'Hide Details';
    } else {
        details.classList.add('hidden');
        this.textContent = 'Details';
    }
});

function delaySuccess() {
    document.getElementById('loading').style.display = 'block';
    document.getElementById('success').style.display = 'none';

    setTimeout(() => {
        // Hide loading and show success
        document.getElementById('loading').style.display = 'none';
        document.getElementById('success').style.display = 'block';
    }, 15000);
}

window.onload = delaySuccess;
