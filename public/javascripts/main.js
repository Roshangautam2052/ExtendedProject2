// Updates the copyright year in footer with the current year
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