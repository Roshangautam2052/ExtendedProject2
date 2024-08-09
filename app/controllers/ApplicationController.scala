package controllers

import play.api.mvc.ControllerComponents

import javax.inject._

@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents
                                     ){

}
