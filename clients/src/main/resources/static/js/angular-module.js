const app = angular.module('bookingApp', ['ui.bootstrap', 'toastr']);

let httpHeaders = {
        headers : {
            "Content-Type": "application/x-www-form-urlencoded"
        }
    };



app.controller('AppController', function($http, toastr, $uibModal) {
    const demoApp = this;
    const apiBaseURL = "/api/booking/";

    demoApp.landingScreen = true;
    demoApp.homeScreen = false;
    demoApp.activeParty = "Bank";
    demoApp.assetMap = {};
    demoApp.balance = 0;
    demoApp.showSpinner = false;


    demoApp.setupData = () => {
        demoApp.showSpinner = true;
        $http.post(apiBaseURL + 'setup', httpHeaders)
        .then((response) => {
            if(response.data && response.data.status){
                toastr.success('Data Setup Successful!');
                demoApp.landingScreen = false;
                demoApp.homeScreen = true;
                demoApp.buttonScreen = true;
                demoApp.fetchAssets();
                demoApp.fetchBalance();
            }else{
                toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
            }
            demoApp.showSpinner = false;
        });
    }



    demoApp.openDemo = () =>{
      var partyName = $("#dropdownMenu").find("option:selected").text();
       demoApp.landingScreen = false;
       demoApp.identityPage = true;
       demoApp.buttonScreen = true;
       demoApp.activeParty = partyName;
       demoApp.getBalance();
    }

    demoApp.createVenue = () =>{
    const venueModal = $uibModal.open({
                           templateUrl: 'addNewVenue.html',
                           controller: 'NewVenueCtrl',
                           controllerAs: 'newVenueCtrl',
                           resolve: {
                               demoApp: () => demoApp,
                               apiBaseURL: () => apiBaseURL,
                               toastr: () => toastr,
                           }
               })
               demoApp.getBalance();
    }

    demoApp.issueMoney =() =>{
            const cashModal = $uibModal.open({
                templateUrl: 'issueMoneyModal.html',
                controller: 'MoneyModalCtrl',
                controllerAs: 'moneyModalCtrl',
                resolve: {
                    demoApp: () => demoApp,
                    apiBaseURL: () => apiBaseURL,
                    toastr: () => toastr,
                }
    })
    }
    demoApp.backLogin = ()=> {
           demoApp.landingScreen = true;
           demoApp.identityPage = false;
           demoApp.venueScreen = false;
           demoApp.ticketScreen= false;
           demoApp.requestScreen = false;

           demoApp.activeParty = null;
    }

    demoApp.getBalance = () => {
           demoApp.showAssetSpinner = true;
           $http.post(apiBaseURL + 'getBalance',demoApp.activeParty)
           .then((response) => {
              if(response.data && response.data.status){
                   demoApp.balance = 0;
                   for(let i in response.data.data){
                     if(response.data.data[i].state.data.amount){

                              var num = response.data.data[i].state.data.amount.split(" ")[0];

                              demoApp.balance = demoApp.balance + parseInt(num);
                        }
                     }
                    console.log(demoApp.balance);
              }else{
                 toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
              }
              demoApp.showAssetSpinner = false;
           });
        }

       demoApp.bookTicket = () =>{
       const ticketModal = $uibModal.open({
                       templateUrl: 'ticketModal.html',
                       controller: 'TicketModalCtrl',
                       controllerAs: 'ticketModalCtrl',
                       resolve: {
                           demoApp: () => demoApp,
                           apiBaseURL: () => apiBaseURL,
                           toastr: () => toastr,
                       }
           })
           demoApp.getBalance();
       }

       demoApp.issueTicket = () =>{
              const ticketModal = $uibModal.open({
                              templateUrl: 'requestModal.html',
                              controller: 'RequestModalCtrl',
                              controllerAs: 'requestModalCtrl',
                              resolve: {
                                  demoApp: () => demoApp,
                                  apiBaseURL: () => apiBaseURL,
                                  toastr: () => toastr,
                              }
                  })
              }



    demoApp.switchPage =(pageName) =>{
        demoApp.buttonScreen = false;
       switch(pageName){
       case "Venue":
               demoApp.venueScreen= true;
               demoApp.getVenueList();
               break;
       case "Seat":
               demoApp.ticketScreen= true;
               demoApp.getTicketList();
               break;
       case "Request":
              demoApp.requestScreen = true;
              demoApp.getRequestList();
              break;

       }
       }


    demoApp.getRequestList = () =>{
            demoApp.showSpinner = true;
            $http.get(apiBaseURL + 'getRequestState')
            .then((response) => {
                if(response.data && response.data.status){
                       demoApp.requestList= response.data.data;
                       console.log(demoApp.requestList);
                }else{
                    toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
                }
                demoApp.showSpinner = false;
            });
    }

    demoApp.getVenueList = () =>{
                demoApp.showSpinner = true;
                $http.get(apiBaseURL + 'getVenueState')
                .then((response) => {
                    if(response.data && response.data.status){
                           demoApp.venueList = response.data.data;
                           console.log(demoApp.venueList);
                    }else{
                        toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
                    }
                    demoApp.showSpinner = false;
                });
        }
    demoApp.getTicketList = () =>{
                    demoApp.showSpinner = true;
                    $http.get(apiBaseURL + 'getTicketState')
                    .then((response) => {
                        if(response.data && response.data.status){
                               demoApp.ticketList = response.data.data;
                               angular.forEach(demoApp.ticketList,function(value,index){
                                 var currentData= value.state.data;
                                 var date  = value.state.data.endTime;
                                 var d= new Date();
                                 var compareDate = new Date(Date.parse(date));
                                 var date1 = d.getFullYear() + '-' + ("0" + (d.getMonth() + 1)).slice(-2) + "-" + ("0" + d.getDate()).slice(-2);
                                 console.log(d);
                                 value.state.data.isExpired =  date1 > date;
                               })
                               console.log(demoApp.ticketList);
                        }else{
                            toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
                        }
                        demoApp.showSpinner = false;
                    });
            }
    demoApp.compareDates = (index) =>{
    var date = demoApp.ticketList.get(index).state.data.endTime;
    console.log(date);
    if(date == null)
    return false;
    date = date.replace('T', ' ');
    console.log(Date);
    var compareDate = Date(date).getTime();
     console.log(compareDate);
     var d= new Date();
     var date1 = d.getFullYear() + '-' + ("0" + (d.getMonth() + 1)).slice(-2) + "-" + ("0" + d.getDate()).slice(-2);
    return date1 > compareDate;
    }



});



app.controller('NewVenueCtrl', function ($http, $uibModalInstance, $uibModal, demoApp, apiBaseURL, toastr) {
    const createNewVenueModel = this;
    createNewVenueModel.form = {};

    createNewVenueModel.create = () => {
        if(createNewVenueModel.form.imgUrl == '' || createNewVenueModel.form.venueId == undefined ||
        createNewVenueModel.form.description == '' || createNewVenueModel.form.startTime == '' ||
        createNewVenueModel.form.endTime == ''|| createNewVenueModel.form.price == '' || createNewVenueModel.form.description == '' || createNewVenueModel.form.buyer == ''
        || createNewVenueModel.form.maxSeat == ''){
           toastr.error("All fields are mandatory!");
        }else{
           demoApp.showSpinner = true;
           $http.post(apiBaseURL + 'createVenue', createNewVenueModel.form)
           .then((response) => {
              if(response.data && response.data.status){
                  toastr.success('Venue Successfully');
                  $uibModalInstance.dismiss();
              }else{
                  toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
              }
               demoApp.getTicketList();
              demoApp.showSpinner = false;
           });
        }
    }

    createNewVenueModel.cancel = () => $uibModalInstance.dismiss();

});

app.controller('MoneyModalCtrl', function ($http, $uibModalInstance, $uibModal, demoApp, apiBaseURL, toastr) {
    const moneyModal = this;

    moneyModal.form = {};
    moneyModal.form.party = "Bank";
    moneyModal.issueMoney = () => {
        if(moneyModal.form.amount == undefined){
           toastr.error("Please enter amount to be issued");
        }else{
            demoApp.showSpinner = true;
            $http.post(apiBaseURL + 'issueMoney', moneyModal.form)
            .then((response) => {
               if(response.data && response.data.status){
                   toastr.success('Money Issued Successfully');
                   $uibModalInstance.dismiss();
               }else{
                   toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
               }
               demoApp.showSpinner = false;
            });
        }
    }

    moneyModal.cancel = () => $uibModalInstance.dismiss();

});

app.controller('TicketModalCtrl', function ($http, $uibModalInstance, $uibModal, demoApp, apiBaseURL, toastr) {
    const ticketModal = this;

    ticketModal.bookForm = {};
    ticketModal.bookForm.agency = "Agency"

    ticketModal.book = () => {
        if(ticketModal.bookForm.venueId == undefined){
           toastr.error("Please enter the venueId you want to book");
        }else{
            price = demoApp.venueList.filter( it => it.state.data.venueId == ticketModal.bookForm.venueId)[0].state.data.price;
            ticketModal.bookForm.price =price.split(" ")[0]
            console.log(ticketModal.bookForm.price);
            demoApp.showSpinner = true;
            $http.post(apiBaseURL + 'requestTicket', ticketModal.bookForm)
            .then((response) => {
               if(response.data && response.data.status){
                   toastr.success('Request Generated');
                   demoApp.getBalance();
                   $uibModalInstance.dismiss();
               }else{
                   toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
               }
               demoApp.showSpinner = false;
            });
        }
    }

    ticketModal.cancel = () => $uibModalInstance.dismiss();

});


app.controller('RequestModalCtrl', function ($http, $uibModalInstance, $uibModal, demoApp, apiBaseURL, toastr) {
    const requestModal = this;

    requestModal.form = {};

    requestModal.issue = () => {
        if(requestModal.form.requestId == undefined){
           toastr.error("Please enter the requestId");
        }else{
            demoApp.showSpinner = true;
            $http.post(apiBaseURL + 'issueTicket',requestModal.form)
            .then((response) => {
               if(response.data && response.data.status){
                   toastr.success('Ticket Issued');
                   demoApp.getBalance();
                   $uibModalInstance.dismiss();
               }else{
                   toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
               }
               demoApp.showSpinner = false;
            });
        }
    }

    requestModal.cancel = () => $uibModalInstance.dismiss();

});

app.controller('BookVModalCtrl', function ($http, $uibModalInstance, $uibModal, demoApp, apiBaseURL, toastr) {
    const requestModal = this;

    requestModal.form = {};

    requestModal.issueTicket = () => {
        if(ticketModal.form.requestId == undefined){
           toastr.error("Please enter the requestId");
        }else{
            demoApp.showSpinner = true;
            $http.post(apiBaseURL + 'issueTicket',requestModal.form)
            .then((response) => {
               if(response.data && response.data.status){
                   toastr.success('Ticket Issued');
                   demoApp.getBalance();
                   $uibModalInstance.dismiss();
               }else{
                   toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
               }
               demoApp.showSpinner = false;
            });
        }
    }

    requestModal.cancel = () => $uibModalInstance.dismiss();

});





